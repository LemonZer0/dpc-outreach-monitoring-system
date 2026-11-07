package com.dpcoutreach.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleSheetsSyncServiceTest {

    private final GoogleSheetsSnapshotService snapshotService = mock(GoogleSheetsSnapshotService.class);
    private final GoogleSheetsPublisher publisher = mock(GoogleSheetsPublisher.class);
    private GoogleSheetsSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new GoogleSheetsSyncService("spreadsheet-id", "credentials.json", snapshotService, publisher);
    }

    @Test
    void publishesSnapshotAndRecordsSuccessfulSync() throws IOException {
        List<GoogleSheetsSnapshotService.SheetSnapshot> snapshot = List.of(
                new GoogleSheetsSnapshotService.SheetSnapshot("Companies", List.of("ID", "Name"), List.of()));
        when(snapshotService.createSnapshot()).thenReturn(snapshot);

        GoogleSheetsSyncService.SyncStatus status = syncService.syncNow();

        assertEquals("SUCCESS", status.state());
        assertEquals("All managed tabs were synchronized", status.message());
        verify(publisher).publish("spreadsheet-id", snapshot);
    }

    @Test
    void recordsFailureAndExposesRetryableError() throws IOException {
        when(snapshotService.createSnapshot()).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new IOException("credentials rejected"))
                .when(publisher).publish("spreadsheet-id", List.of());

        GoogleSheetsSyncException exception = assertThrows(
                GoogleSheetsSyncException.class, () -> syncService.syncNow());

        assertEquals("Google Sheets mirror sync failed: credentials rejected", exception.getMessage());
        assertEquals("FAILED", syncService.getStatus().state());
        assertEquals("credentials rejected", exception.getCause().getMessage());
    }

    @Test
    void requiresSpreadsheetAndCredentialsBeforeSyncing() {
        GoogleSheetsSyncService unconfigured = new GoogleSheetsSyncService("", "", snapshotService, publisher);

        assertThrows(GoogleSheetsSyncException.class, unconfigured::syncNow);
        assertEquals("DISABLED", unconfigured.getStatus().state());
    }
}
