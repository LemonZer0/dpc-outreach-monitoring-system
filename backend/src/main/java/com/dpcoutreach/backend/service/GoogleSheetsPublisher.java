package com.dpcoutreach.backend.service;

import java.io.IOException;
import java.util.List;

public interface GoogleSheetsPublisher {
    void publish(String spreadsheetId, List<GoogleSheetsSnapshotService.SheetSnapshot> sheets) throws IOException;
}
