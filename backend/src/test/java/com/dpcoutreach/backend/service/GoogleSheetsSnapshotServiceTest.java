package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.CallSource;
import com.dpcoutreach.backend.entity.CallType;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.entity.VerificationStatus;
import com.dpcoutreach.backend.repository.CallRecordRepository;
import com.dpcoutreach.backend.repository.CompanyRepository;
import com.dpcoutreach.backend.repository.ContactRepository;
import com.dpcoutreach.backend.repository.CoreOutreachStatusRepository;
import com.dpcoutreach.backend.repository.DpcRepository;
import com.dpcoutreach.backend.repository.OutreachStatusRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleSheetsSnapshotServiceTest {

    @Test
    void includesSimulationCallsInSharedCallRecordsSheetWithSourceTag() {
        CallRecordRepository callRecordRepository = mock(CallRecordRepository.class);
        DpcRepository dpcRepository = mock(DpcRepository.class);
        ContactRepository contactRepository = mock(ContactRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        OutreachStatusRepository outreachStatusRepository = mock(OutreachStatusRepository.class);
        CoreOutreachStatusRepository coreOutreachStatusRepository = mock(CoreOutreachStatusRepository.class);

        CallRecord simulatedCall = mock(CallRecord.class);
        Dpc dpc = mock(Dpc.class);
        when(simulatedCall.getId()).thenReturn(41L);
        when(simulatedCall.getDpc()).thenReturn(dpc);
        when(dpc.getId()).thenReturn(7L);
        when(dpc.getName()).thenReturn("DPC A");
        when(simulatedCall.getPhoneNumber()).thenReturn("1234567890");
        when(simulatedCall.getCallTimestamp()).thenReturn(Instant.parse("2026-11-08T10:00:00Z"));
        when(simulatedCall.getDurationSeconds()).thenReturn(45);
        when(simulatedCall.getVerificationStatus()).thenReturn(VerificationStatus.UNMATCHED_NUMBER);
        when(simulatedCall.getCallSource()).thenReturn(CallSource.SIMULATION);
        when(simulatedCall.getCallType()).thenReturn(null);
        when(simulatedCall.getCallOutcome()).thenReturn(null);
        when(callRecordRepository.findAllByOrderByCallTimestampDesc()).thenReturn(List.of(simulatedCall));
        when(dpcRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        when(contactRepository.findAllWithCompanyOrderByCompanyNameAndName()).thenReturn(List.of());
        when(companyRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        when(outreachStatusRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of());
        when(coreOutreachStatusRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of());

        List<GoogleSheetsSnapshotService.SheetSnapshot> snapshot =
                new GoogleSheetsSnapshotService(
                        callRecordRepository,
                        dpcRepository,
                        contactRepository,
                        companyRepository,
                        outreachStatusRepository,
                        coreOutreachStatusRepository)
                        .createSnapshot();

        assertEquals(
                List.of("Call records", "DPCs", "Contacts", "Companies",
                        "DPC outreach status", "CORE outreach decisions"),
                snapshot.stream().map(GoogleSheetsSnapshotService.SheetSnapshot::title).toList());
        assertEquals("SIMULATION", snapshot.getFirst().rows().getFirst().get(11));
        assertEquals("", snapshot.getFirst().rows().getFirst().get(12));
    }
}
