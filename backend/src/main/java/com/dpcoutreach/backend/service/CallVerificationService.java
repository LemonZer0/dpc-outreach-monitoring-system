package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.dto.CallRecordMatchRequest;
import com.dpcoutreach.backend.dto.CallSubmissionRequest;
import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.CallSource;
import com.dpcoutreach.backend.entity.CallType;
import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.entity.OutreachStatus;
import com.dpcoutreach.backend.entity.VerificationStatus;
import com.dpcoutreach.backend.repository.CallRecordRepository;
import com.dpcoutreach.backend.repository.CompanyRepository;
import com.dpcoutreach.backend.repository.ContactRepository;
import com.dpcoutreach.backend.repository.DpcRepository;
import com.dpcoutreach.backend.repository.OutreachStatusRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CallVerificationService {

    private final DpcRepository dpcRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final ContactLookupService contactLookupService;
    private final CallRecordRepository callRecordRepository;
    private final OutreachStatusRepository outreachStatusRepository;

    public CallVerificationService(
            DpcRepository dpcRepository,
            CompanyRepository companyRepository,
            ContactRepository contactRepository,
            ContactLookupService contactLookupService,
            CallRecordRepository callRecordRepository,
            OutreachStatusRepository outreachStatusRepository) {
        this.dpcRepository = dpcRepository;
        this.companyRepository = companyRepository;
        this.contactRepository = contactRepository;
        this.contactLookupService = contactLookupService;
        this.callRecordRepository = callRecordRepository;
        this.outreachStatusRepository = outreachStatusRepository;
    }

    @Transactional
    public CallRecord verifyAndSave(CallSubmissionRequest request) {
        CallType callType = request == null || request.callType() == null
                ? CallType.MANUAL_METADATA
                : request.callType();
        return verifyAndSave(request, CallSource.APP, callType);
    }

    @Transactional
    public CallRecord verifyAndSaveSimulation(CallSubmissionRequest request) {
        return verifyAndSave(request, CallSource.SIMULATION, null);
    }

    private CallRecord verifyAndSave(CallSubmissionRequest request, CallSource callSource, CallType callType) {
        validateRequest(request);
        Dpc dpc = dpcRepository.findByIdForUpdate(request.dpcId())
                .orElseThrow(() -> new DpcNotFoundException(request.dpcId()));

        String normalizedPhoneNumber = PhoneNumberNormalizer.normalize(request.phoneNumber());
        boolean duplicate = callRecordRepository
                .existsByDpcIdAndCallSourceAndPhoneNumberAndCallTimestampAndDurationSeconds(
                        request.dpcId(), callSource, normalizedPhoneNumber,
                        request.callTimestamp(), request.durationSeconds());
        List<Contact> matchingContacts = contactLookupService.findMatchesByPhoneNumber(normalizedPhoneNumber);

        Contact contact = matchingContacts.size() == 1 ? matchingContacts.getFirst() : null;

        VerificationStatus verificationStatus = classify(contact, request.durationSeconds(), duplicate);
        CallRecord callRecord = callRecordRepository.save(new CallRecord(
                dpc,
                contact,
                normalizedPhoneNumber,
                request.callTimestamp(),
                request.durationSeconds(),
                verificationStatus,
                callSource,
                callType,
                request.callOutcome()));

        if (contact != null && !duplicate && callSource == CallSource.APP) {
            updateOutreachStatus(contact, callRecord, verificationStatus);
        }

        return callRecord;
    }

    @Transactional
    public CallRecord matchUnmatchedCall(Long callRecordId, CallRecordMatchRequest request) {
        if (request == null || request.contactName() == null || request.contactName().isBlank()
                || request.companyId() == null) {
            throw new IllegalArgumentException("Contact name and company are required");
        }

        CallRecord callRecord = callRecordRepository.findByIdForUpdate(callRecordId)
                .orElseThrow(() -> new CallRecordNotFoundException(callRecordId));
        if (callRecord.getContact() != null
                || callRecord.getVerificationStatus() == VerificationStatus.DUPLICATE_CALL) {
            throw new CallRecordMatchConflictException(
                    "Call record with id " + callRecordId + " is no longer eligible for manual matching");
        }

        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new AdminResourceNotFoundException("Company", request.companyId()));
        String normalizedPhoneNumber = PhoneNumberNormalizer.normalize(callRecord.getPhoneNumber());
        Contact contact = contactRepository.save(new Contact(
                request.contactName().trim(), normalizedPhoneNumber, company));

        List<CallRecord> unmatchedCalls = callRecordRepository
                .findAllByPhoneNumberAndContactIsNullAndVerificationStatusNot(
                        normalizedPhoneNumber, VerificationStatus.DUPLICATE_CALL);
        for (CallRecord unmatchedCall : unmatchedCalls) {
            VerificationStatus status = classify(contact, unmatchedCall.getDurationSeconds(), false);
            unmatchedCall.setContact(contact);
            unmatchedCall.setVerificationStatus(status);
            callRecordRepository.save(unmatchedCall);
            if (unmatchedCall.getCallSource() == CallSource.APP) {
                updateOutreachStatus(contact, unmatchedCall, status);
            }
        }
        return callRecord;
    }

    private void validateRequest(CallSubmissionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Call request must not be null");
        }
        if (request.dpcId() == null) {
            throw new IllegalArgumentException("DPC ID is required");
        }
        if (request.callTimestamp() == null) {
            throw new IllegalArgumentException("Call timestamp is required");
        }
        if (request.durationSeconds() == null || request.durationSeconds() < 0) {
            throw new IllegalArgumentException("Call duration must be zero or greater");
        }
    }

    private VerificationStatus classify(Contact contact, Integer durationSeconds, boolean duplicate) {
        if (duplicate) {
            return VerificationStatus.DUPLICATE_CALL;
        }
        if (contact == null) {
            return VerificationStatus.UNMATCHED_NUMBER;
        }
        if (durationSeconds > 60) {
            return VerificationStatus.MATCHED_LONG_CALL;
        }
        return durationSeconds > 0
                ? VerificationStatus.MATCHED_CONNECTED
                : VerificationStatus.MATCHED_NOT_CONNECTED;
    }

    private void updateOutreachStatus(Contact contact, CallRecord callRecord, VerificationStatus verificationStatus) {
        Long companyId = contact.getCompany().getId();
        Optional<OutreachStatus> existingStatus = outreachStatusRepository.findByCompanyId(companyId);
        if (existingStatus.isEmpty()) {
            outreachStatusRepository.save(new OutreachStatus(
                    contact.getCompany(), verificationStatus.name(), callRecord, Instant.now()));
            return;
        }

        OutreachStatus status = existingStatus.get();
        CallRecord latestCall = status.getLatestCallRecord();
        if (latestCall == null || callRecord.getCallTimestamp().isAfter(latestCall.getCallTimestamp())) {
            status.setStatus(verificationStatus.name());
            status.setLatestCallRecord(callRecord);
            status.setUpdatedAt(Instant.now());
            outreachStatusRepository.save(status);
        }
    }
}
