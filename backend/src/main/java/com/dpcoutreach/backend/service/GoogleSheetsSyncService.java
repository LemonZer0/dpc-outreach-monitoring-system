package com.dpcoutreach.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.Instant;

@Service
public class GoogleSheetsSyncService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsSyncService.class);

    private final String spreadsheetId;
    private final String credentialsFile;
    private final GoogleSheetsSnapshotService snapshotService;
    private final GoogleSheetsPublisher publisher;
    private volatile SyncStatus status = new SyncStatus("DISABLED", null, null, "Google Sheets is not configured");

    public GoogleSheetsSyncService(
            @Value("${google.sheets.spreadsheet-id:}") String spreadsheetId,
            @Value("${google.sheets.credentials-file:}") String credentialsFile,
            GoogleSheetsSnapshotService snapshotService,
            GoogleSheetsPublisher publisher) {
        this.spreadsheetId = spreadsheetId;
        this.credentialsFile = credentialsFile;
        this.snapshotService = snapshotService;
        this.publisher = publisher;
    }

    public boolean isConfigured() {
        return !spreadsheetId.isBlank() && !credentialsFile.isBlank();
    }

    public SyncStatus getStatus() {
        return status;
    }

    public synchronized SyncStatus syncNow() {
        if (!isConfigured()) {
            status = new SyncStatus("DISABLED", null, null,
                    "Set GOOGLE_SHEETS_SPREADSHEET_ID and GOOGLE_SHEETS_CREDENTIALS_FILE to enable sync");
            throw new GoogleSheetsSyncException(status.message(), null);
        }

        Instant attemptedAt = Instant.now();
        status = new SyncStatus("SYNCING", attemptedAt, status.lastSuccessfulAt(), "Sync in progress");
        try {
            publisher.publish(spreadsheetId, snapshotService.createSnapshot());
            status = new SyncStatus("SUCCESS", attemptedAt, Instant.now(), "All managed tabs were synchronized");
            logger.info("Google Sheets mirror sync succeeded");
            return status;
        } catch (IOException | RestClientException | DataAccessException
                 | IllegalArgumentException | IllegalStateException | SecurityException exception) {
            String message = "Google Sheets mirror sync failed: " + exception.getMessage();
            status = new SyncStatus("FAILED", attemptedAt, status.lastSuccessfulAt(), message);
            throw new GoogleSheetsSyncException(message, exception);
        }
    }

    public record SyncStatus(String state, Instant lastAttemptedAt, Instant lastSuccessfulAt, String message) {}
}
