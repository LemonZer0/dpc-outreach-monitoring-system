package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.CallSource;
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
    void deletesUnlinkedCallRecords() {
        CallRecord duplicate = new CallRecord(
                null, null, "919876543210", Instant.now(), 10, VerificationStatus.DUPLICATE_CALL);
        when(callRecordRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(duplicate));

        service.deleteCallRecord(42L);

        verify(callRecordRepository).delete(duplicate);
    }

    @Test
    void deletesLinkedCallAndRepointsLatestStatusToMostRecentRemainingAppCall() {
        var company = mock(Company.class);
        var contact = mock(Contact.class);
        var callRecord = mock(CallRecord.class);
        var latestCall = mock(CallRecord.class);
        var replacementCall = mock(CallRecord.class);
        var status = mock(OutreachStatus.class);
        when(callRecordRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(callRecord));
        when(callRecord.getContact()).thenReturn(contact);
        when(callRecord.getId()).thenReturn(42L);
        when(contact.getCompany()).thenReturn(company);
        when(company.getId()).thenReturn(12L);
        when(status.getCompany()).thenReturn(company);
        when(outreachStatusRepository.findByLatestCallRecord_Id(42L)).thenReturn(Optional.of(status));
        when(status.getLatestCallRecord()).thenReturn(latestCall);
        when(latestCall.getId()).thenReturn(42L);
        when(callRecordRepository
                .findFirstByContact_Company_IdAndCallSourceAndVerificationStatusInAndIdNotOrderByCallTimestampDesc(
                        12L,
                        CallSource.APP,
                        List.of(
                                VerificationStatus.MATCHED_CONNECTED,
                                VerificationStatus.MATCHED_LONG_CALL,
                                VerificationStatus.MATCHED_NOT_CONNECTED),
                        42L))
                .thenReturn(Optional.of(replacementCall));
        when(replacementCall.getVerificationStatus()).thenReturn(VerificationStatus.MATCHED_LONG_CALL);

        service.deleteCallRecord(42L);

        verify(status).setLatestCallRecord(replacementCall);
        verify(status).setStatus(VerificationStatus.MATCHED_LONG_CALL.name());
        verify(outreachStatusRepository).saveAndFlush(status);
        verify(callRecordRepository).delete(callRecord);
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
