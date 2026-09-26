package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.dto.CallSubmissionRequest;
import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.entity.OutreachStatus;
import com.dpcoutreach.backend.entity.VerificationStatus;
import com.dpcoutreach.backend.repository.CallRecordRepository;
import com.dpcoutreach.backend.repository.ContactRepository;
import com.dpcoutreach.backend.repository.DpcRepository;
import com.dpcoutreach.backend.repository.OutreachStatusRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallVerificationServiceTest {

    private static final Long DPC_ID = 1L;
    private static final String FORMATTED_PHONE = "+91 (98765) 432-10";
    private static final String NORMALIZED_PHONE = "919876543210";
    private static final Instant CALL_TIME = Instant.parse("2026-10-13T10:00:00Z");

    @Test
    void savesConnectedCallAndUpdatesCompanyStatus() {
        Contact contact = contact();
        TestHarness harness = new TestHarness(List.of(contact), false);

        CallRecord saved = harness.service.verifyAndSave(request(45));

        assertEquals(VerificationStatus.MATCHED_CONNECTED, saved.getVerificationStatus());
        assertEquals(contact, saved.getContact());
        assertEquals(NORMALIZED_PHONE, saved.getPhoneNumber());
        verify(harness.callRecordRepository).save(saved);
        verify(harness.outreachStatusRepository).save(any(OutreachStatus.class));
    }

    @Test
    void savesKnownContactWithZeroDurationAsNotConnected() {
        TestHarness harness = new TestHarness(List.of(contact()), false);

        CallRecord saved = harness.service.verifyAndSave(request(0));

        assertEquals(VerificationStatus.MATCHED_NOT_CONNECTED, saved.getVerificationStatus());
    }

    @Test
    void savesUnknownNumberWithoutContactOrOutreachUpdate() {
        TestHarness harness = new TestHarness(List.of(), false);

        CallRecord saved = harness.service.verifyAndSave(request(20));

        assertEquals(VerificationStatus.UNMATCHED_NUMBER, saved.getVerificationStatus());
        assertNull(saved.getContact());
        verify(harness.outreachStatusRepository, never()).save(any(OutreachStatus.class));
    }

    @Test
    void savesDuplicateWithDuplicateStatusAndDoesNotUpdateOutreachStatus() {
        TestHarness harness = new TestHarness(List.of(contact()), true);

        CallRecord saved = harness.service.verifyAndSave(request(20));

        assertEquals(VerificationStatus.DUPLICATE_CALL, saved.getVerificationStatus());
        verify(harness.outreachStatusRepository, never()).save(any(OutreachStatus.class));
    }

    @Test
    void rejectsPhoneNumberMatchingMultipleContacts() {
        Contact contact = contact();
        TestHarness harness = new TestHarness(List.of(contact, contact), false);

        assertThrows(AmbiguousContactMatchException.class, () -> harness.service.verifyAndSave(request(20)));
        verify(harness.callRecordRepository, never()).save(any(CallRecord.class));
    }

    @Test
    void duplicateCheckUsesDpcNumberTimestampAndDuration() {
        TestHarness harness = new TestHarness(List.of(contact()), true);

        harness.service.verifyAndSave(request(20));

        verify(harness.callRecordRepository).existsByDpcIdAndPhoneNumberAndCallTimestampAndDurationSeconds(
                DPC_ID, NORMALIZED_PHONE, CALL_TIME, 20);
    }

    @Test
    void olderMatchedCallDoesNotReplaceLatestOutreachStatus() {
        Contact contact = contact();
        TestHarness harness = new TestHarness(List.of(contact), false);
        CallRecord newerCall = new CallRecord(
                null, contact, NORMALIZED_PHONE, CALL_TIME.plusSeconds(60), 30,
                VerificationStatus.MATCHED_CONNECTED);
        OutreachStatus existingStatus = new OutreachStatus(
                contact.getCompany(), VerificationStatus.MATCHED_CONNECTED.name(), newerCall, CALL_TIME);
        when(harness.outreachStatusRepository.findByCompanyId(isNull())).thenReturn(Optional.of(existingStatus));

        harness.service.verifyAndSave(request(20));

        assertEquals(newerCall, existingStatus.getLatestCallRecord());
        assertEquals(VerificationStatus.MATCHED_CONNECTED.name(), existingStatus.getStatus());
        verify(harness.outreachStatusRepository, never()).save(any(OutreachStatus.class));
    }

    @Test
    void rejectsUnknownDpc() {
        TestHarness harness = new TestHarness(List.of(), false);
        when(harness.dpcRepository.findByIdForUpdate(DPC_ID)).thenReturn(Optional.empty());

        assertThrows(DpcNotFoundException.class, () -> harness.service.verifyAndSave(request(20)));
        verify(harness.callRecordRepository, never()).save(any(CallRecord.class));
    }

    @Test
    void rejectsNegativeDurationBeforeAccessingRepositories() {
        TestHarness harness = new TestHarness(List.of(), false);

        assertThrows(IllegalArgumentException.class,
                () -> harness.service.verifyAndSave(request(-1)));
        verify(harness.dpcRepository, never()).findByIdForUpdate(DPC_ID);
    }

    @Test
    void rejectsMissingRequestFieldsBeforeAccessingRepositories() {
        TestHarness harness = new TestHarness(List.of(), false);
        CallSubmissionRequest request = new CallSubmissionRequest(DPC_ID, FORMATTED_PHONE, null, null);

        assertThrows(IllegalArgumentException.class, () -> harness.service.verifyAndSave(request));
        verify(harness.dpcRepository, never()).findByIdForUpdate(DPC_ID);
    }

    private static CallSubmissionRequest request(int durationSeconds) {
        return new CallSubmissionRequest(DPC_ID, FORMATTED_PHONE, CALL_TIME, durationSeconds);
    }

    private static Contact contact() {
        return new Contact("Company Contact", NORMALIZED_PHONE, new Company("Example Company"));
    }

    private static class TestHarness {
        private final DpcRepository dpcRepository = mock(DpcRepository.class);
        private final ContactRepository contactRepository = mock(ContactRepository.class);
        private final CallRecordRepository callRecordRepository = mock(CallRecordRepository.class);
        private final OutreachStatusRepository outreachStatusRepository = mock(OutreachStatusRepository.class);
        private final CallVerificationService service;

        private TestHarness(List<Contact> contacts, boolean duplicate) {
            when(dpcRepository.findByIdForUpdate(DPC_ID)).thenReturn(Optional.of(new Dpc("Test DPC", "test")));
            when(contactRepository.findAllByNormalizedPhoneNumber(NORMALIZED_PHONE)).thenReturn(contacts);
            when(callRecordRepository.existsByDpcIdAndPhoneNumberAndCallTimestampAndDurationSeconds(
                    eq(DPC_ID), eq(NORMALIZED_PHONE), eq(CALL_TIME), anyInt())).thenReturn(duplicate);
            when(callRecordRepository.save(any(CallRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(outreachStatusRepository.findByCompanyId(isNull())).thenReturn(Optional.empty());
            when(outreachStatusRepository.save(any(OutreachStatus.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ContactLookupService contactLookupService = new ContactLookupService(contactRepository);
            service = new CallVerificationService(
                    dpcRepository, contactLookupService, callRecordRepository, outreachStatusRepository);
        }
    }
}
