package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.dto.CompanyResponse;
import com.dpcoutreach.backend.dto.CompanyWriteRequest;
import com.dpcoutreach.backend.dto.ContactResponse;
import com.dpcoutreach.backend.dto.ContactWriteRequest;
import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.repository.CallRecordRepository;
import com.dpcoutreach.backend.repository.CompanyRepository;
import com.dpcoutreach.backend.repository.ContactRepository;
import com.dpcoutreach.backend.repository.OutreachStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminManagementService {

    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final CallRecordRepository callRecordRepository;
    private final OutreachStatusRepository outreachStatusRepository;

    public AdminManagementService(
            CompanyRepository companyRepository,
            ContactRepository contactRepository,
            CallRecordRepository callRecordRepository,
            OutreachStatusRepository outreachStatusRepository) {
        this.companyRepository = companyRepository;
        this.contactRepository = contactRepository;
        this.callRecordRepository = callRecordRepository;
        this.outreachStatusRepository = outreachStatusRepository;
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> findAllCompanies() {
        return companyRepository.findAllByOrderByNameAsc().stream()
                .map(CompanyResponse::from)
                .toList();
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
        if (contactRepository.existsByCompany_Id(companyId)) {
            throw new AdminResourceConflictException(
                    "Company with id " + companyId + " cannot be deleted while it has contacts");
        }
        if (outreachStatusRepository.findByCompanyId(companyId).isPresent()) {
            throw new AdminResourceConflictException(
                    "Company with id " + companyId + " cannot be deleted because it has outreach history");
        }
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
        if (callRecordRepository.existsByContact_Id(contactId)) {
            throw new AdminResourceConflictException(
                    "Contact with id " + contactId + " cannot be deleted because it has call history");
        }
        contactRepository.delete(contact);
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
