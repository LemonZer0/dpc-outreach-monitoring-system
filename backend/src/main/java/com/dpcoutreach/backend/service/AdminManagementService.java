package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.dto.CompanyResponse;
import com.dpcoutreach.backend.dto.CompanyWriteRequest;
import com.dpcoutreach.backend.dto.ContactResponse;
import com.dpcoutreach.backend.dto.ContactWriteRequest;
import com.dpcoutreach.backend.dto.DpcResponse;
import com.dpcoutreach.backend.dto.DpcWriteRequest;
import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.entity.VerificationStatus;
import com.dpcoutreach.backend.repository.CallRecordRepository;
import com.dpcoutreach.backend.repository.CompanyRepository;
import com.dpcoutreach.backend.repository.CoreOutreachStatusRepository;
import com.dpcoutreach.backend.repository.ContactRepository;
import com.dpcoutreach.backend.repository.DpcRepository;
import com.dpcoutreach.backend.repository.OutreachStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminManagementService {

    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final DpcRepository dpcRepository;
    private final CallRecordRepository callRecordRepository;
    private final OutreachStatusRepository outreachStatusRepository;
    private final CoreOutreachStatusRepository coreOutreachStatusRepository;

    public AdminManagementService(
            CompanyRepository companyRepository,
            ContactRepository contactRepository,
            DpcRepository dpcRepository,
            CallRecordRepository callRecordRepository,
            OutreachStatusRepository outreachStatusRepository,
            CoreOutreachStatusRepository coreOutreachStatusRepository) {
        this.companyRepository = companyRepository;
        this.contactRepository = contactRepository;
        this.dpcRepository = dpcRepository;
        this.callRecordRepository = callRecordRepository;
        this.outreachStatusRepository = outreachStatusRepository;
        this.coreOutreachStatusRepository = coreOutreachStatusRepository;
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> findAllCompanies() {
        return companyRepository.findAllByOrderByNameAsc().stream()
                .map(CompanyResponse::from)
                .toList();
    }

    @Transactional
    public DpcResponse createDpc(DpcWriteRequest request) {
        Dpc dpc = dpcRepository.save(new Dpc(request.name().trim(), null));
        return DpcResponse.from(dpc);
    }

    @Transactional
    public CompanyResponse createCompany(CompanyWriteRequest request) {
        return CompanyResponse.from(companyRepository.save(new Company(request.name().trim())));
    }

    @Transactional
    public CompanyResponse updateCompany(Long companyId, CompanyWriteRequest request) {
        Company company = findCompany(companyId);
        company.setName(request.name().trim());
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional
    public void deleteCompany(Long companyId) {
        Company company = findCompany(companyId);
        List<Contact> contacts = contactRepository.findAllByCompany_Id(companyId);
        for (Contact contact : contacts) {
            callRecordRepository.clearContactReference(
                    contact.getId(), VerificationStatus.UNMATCHED_NUMBER);
        }
        contactRepository.deleteAll(contacts);
        outreachStatusRepository.findByCompanyId(companyId).ifPresent(outreachStatusRepository::delete);
        coreOutreachStatusRepository.findByCompanyId(companyId).ifPresent(coreOutreachStatusRepository::delete);
        companyRepository.delete(company);
    }

    @Transactional
    public ContactResponse createContact(ContactWriteRequest request) {
        Company company = findCompany(request.companyId());
        String phoneNumber = PhoneNumberNormalizer.normalize(request.phoneNumber());
        Contact contact = new Contact(request.name().trim(), phoneNumber, company);
        return ContactResponse.from(contactRepository.save(contact));
    }

    @Transactional
    public ContactResponse updateContact(Long contactId, ContactWriteRequest request) {
        Contact contact = findContact(contactId);
        Company company = findCompany(request.companyId());
        contact.setName(request.name().trim());
        contact.setPhoneNumber(PhoneNumberNormalizer.normalize(request.phoneNumber()));
        contact.setCompany(company);
        return ContactResponse.from(contactRepository.save(contact));
    }

    @Transactional
    public void deleteContact(Long contactId) {
        Contact contact = findContact(contactId);
        callRecordRepository.clearContactReference(contactId, VerificationStatus.UNMATCHED_NUMBER);
        contactRepository.delete(contact);
    }

    @Transactional
    public void deleteDuplicateCallRecord(Long callRecordId) {
        CallRecord callRecord = callRecordRepository.findByIdForUpdate(callRecordId)
                .orElseThrow(() -> new CallRecordNotFoundException(callRecordId));
        if (callRecord.getVerificationStatus() != VerificationStatus.DUPLICATE_CALL) {
            throw new AdminResourceConflictException(
                    "Call record with id " + callRecordId + " is not marked as a duplicate");
        }
        callRecordRepository.delete(callRecord);
    }

    private Company findCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Company", companyId));
    }

    private Contact findContact(Long contactId) {
        return contactRepository.findById(contactId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Contact", contactId));
    }
}
