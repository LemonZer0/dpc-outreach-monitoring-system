package com.dpcoutreach.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class GoogleSheetsSyncListener {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsSyncListener.class);

    private final GoogleSheetsSyncService syncService;

    public GoogleSheetsSyncListener(GoogleSheetsSyncService syncService) {
        this.syncService = syncService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeAtStartup() {
        synchronizeIfConfigured();
    }

    @EventListener
    @Async("googleSheetsSyncExecutor")
    public void synchronizeAfterDatabaseChange(GoogleSheetsSyncRequestedEvent event) {
        synchronizeIfConfigured();
    }

    private void synchronizeIfConfigured() {
        if (!syncService.isConfigured()) {
            return;
        }
        try {
            syncService.syncNow();
        } catch (GoogleSheetsSyncException exception) {
            logger.error("{}; use POST /api/admin/google-sheets/sync to retry", exception.getMessage(), exception);
        }
    }
}
