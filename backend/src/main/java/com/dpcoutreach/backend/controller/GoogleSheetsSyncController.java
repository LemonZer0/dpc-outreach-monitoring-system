package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.service.GoogleSheetsSyncService;
import com.dpcoutreach.backend.service.GoogleSheetsSyncService.SyncStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/google-sheets")
public class GoogleSheetsSyncController {

    private final GoogleSheetsSyncService syncService;

    public GoogleSheetsSyncController(GoogleSheetsSyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/status")
    public SyncStatus getStatus() {
        return syncService.getStatus();
    }

    @PostMapping("/sync")
    public SyncStatus synchronizeNow() {
        return syncService.syncNow();
    }
}
