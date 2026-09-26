package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.dto.CallSubmissionRequest;
import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.entity.OutreachStatus;
import com.dpcoutreach.backend.entity.VerificationStatus;
import com.dpcoutreach.backend.repository.CallRecordRepository;
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
    private final ContactLookupService contactLookupService;
    private final CallRecordRepository callRecordRepository;
    private final OutreachStatusRepository outreachStatusRepository;

    public CallVerificationService(
            DpcRepository dpcRepository,
            ContactLookupService contactLookupService,
            CallRecordRepository callRecordRepository,
            OutreachStatusRepository outreachStatusRepository) {
        this.dpcRepository = dpcRepository;
        this.contactLookupService = contactLookupService;
        this.callRecordRepository = callRecordRepository;
        this.outreachStatusRepository = outreachStatusRepository;
    }

    @Transactional
    public CallRecord verifyAndSave(CallSubmissionRequest request) {
        validateRequest(request);
        Dpc dpc = dpcRepository.findByIdForUpdate(request.dpcId())
                .orElseThrow(() -> new DpcNotFoundException(request.dpcId()));

        String normalizedPhoneNumber = PhoneNumberNormalizer.normalize(request.phoneNumber());
        boolean duplicate = callRecordRepository.existsByDpcIdAndPhoneNumberAndCallTimestampAndDurationSeconds(
                request.dpcId(), normalizedPhoneNumber, request.callTimestamp(), request.durationSeconds());
        List<Contact> matchingContacts = contactLookupService.findMatchesByPhoneNumber(normalizedPhoneNumber);
        if (matchingContacts.size() > 1 && !duplicate) {
            throw new AmbiguousContactMatchException(normalizedPhoneNumber);
        }

        Contact contact = matchingContacts.size() == 1 ? matchingContacts.getFirst() : null;

        VerificationStatus verificationStatus = classify(contact, request.durationSeconds(), duplicate);
        CallRecord callRecord = callRecordRepository.save(new CallRecord(
                dpc,
                contact,
                normalizedPhoneNumber,
                request.callTimestamp(),
                request.durationSeconds(),
                verificationStatus));

        if (contact != null && !duplicate) {
            updateOutreachStatus(contact, callRecord, verificationStatus);
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
