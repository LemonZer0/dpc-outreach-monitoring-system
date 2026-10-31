package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.entity.CoreOutreachStatus;
import com.dpcoutreach.backend.entity.OutreachStatus;
import com.dpcoutreach.backend.entity.VerificationStatus;
import com.dpcoutreach.backend.repository.CallRecordRepository;
import com.dpcoutreach.backend.repository.CompanyRepository;
import com.dpcoutreach.backend.repository.ContactRepository;
import com.dpcoutreach.backend.repository.CoreOutreachStatusRepository;
import com.dpcoutreach.backend.repository.DpcRepository;
import com.dpcoutreach.backend.repository.OutreachStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class AdminManagementServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final ContactRepository contactRepository = mock(ContactRepository.class);
    private final DpcRepository dpcRepository = mock(DpcRepository.class);
    private final CallRecordRepository callRecordRepository = mock(CallRecordRepository.class);
    private final OutreachStatusRepository outreachStatusRepository = mock(OutreachStatusRepository.class);
    private final CoreOutreachStatusRepository coreOutreachStatusRepository =
            mock(CoreOutreachStatusRepository.class);
    private AdminManagementService service;

    @BeforeEach
    void setUp() {
        service = new AdminManagementService(
                companyRepository,
                contactRepository,
                dpcRepository,
                callRecordRepository,
                outreachStatusRepository,
                coreOutreachStatusRepository);
    }

    @Test
    void deletesContactAfterDetachingItsCallHistory() {
        Contact contact = mock(Contact.class);
        when(contactRepository.findById(31L)).thenReturn(Optional.of(contact));
        when(contact.getId()).thenReturn(31L);

        service.deleteContact(31L);

        var order = inOrder(callRecordRepository, contactRepository);
        order.verify(callRecordRepository).clearContactReference(31L, VerificationStatus.UNMATCHED_NUMBER);
        order.verify(contactRepository).delete(contact);
    }

    @Test
    void deletesOnlyCallRecordsMarkedAsDuplicates() {
        CallRecord duplicate = new CallRecord(
                null, null, "919876543210", Instant.now(), 10, VerificationStatus.DUPLICATE_CALL);
        when(callRecordRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(duplicate));

        service.deleteDuplicateCallRecord(42L);

        verify(callRecordRepository).delete(duplicate);
    }

    @Test
    void rejectsDeletingCallRecordsThatAreNotDuplicates() {
        CallRecord regularCall = new CallRecord(
                null, null, "919876543210", Instant.now(), 10, VerificationStatus.UNMATCHED_NUMBER);
        when(callRecordRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(regularCall));

        assertThrows(AdminResourceConflictException.class, () -> service.deleteDuplicateCallRecord(42L));

        verify(callRecordRepository, never()).delete(regularCall);
    }

    @Test
    void deletesCompanyContactsAndStatusesAfterDetachingTheirCallHistory() {
        Company company = mock(Company.class);
        Contact firstContact = mock(Contact.class);
        Contact secondContact = mock(Contact.class);
        OutreachStatus outreachStatus = mock(OutreachStatus.class);
        CoreOutreachStatus coreStatus = mock(CoreOutreachStatus.class);
        List<Contact> contacts = List.of(firstContact, secondContact);
        when(companyRepository.findById(12L)).thenReturn(Optional.of(company));
        when(contactRepository.findAllByCompany_Id(12L)).thenReturn(contacts);
        when(firstContact.getId()).thenReturn(31L);
        when(secondContact.getId()).thenReturn(32L);
        when(outreachStatusRepository.findByCompanyId(12L)).thenReturn(Optional.of(outreachStatus));
        when(coreOutreachStatusRepository.findByCompanyId(12L)).thenReturn(Optional.of(coreStatus));

        service.deleteCompany(12L);

        var order = inOrder(callRecordRepository, contactRepository, outreachStatusRepository,
                coreOutreachStatusRepository, companyRepository);
        order.verify(callRecordRepository).clearContactReference(31L, VerificationStatus.UNMATCHED_NUMBER);
        order.verify(callRecordRepository).clearContactReference(32L, VerificationStatus.UNMATCHED_NUMBER);
        order.verify(contactRepository).deleteAll(contacts);
        order.verify(outreachStatusRepository).delete(outreachStatus);
        order.verify(coreOutreachStatusRepository).delete(coreStatus);
        order.verify(companyRepository).delete(company);
    }
}
