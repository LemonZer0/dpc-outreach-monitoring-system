package com.dpcoutreach.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GoogleSheetsApiPublisher implements GoogleSheetsPublisher {

    private static final String SHEETS_API_URL = "https://sheets.googleapis.com/v4/spreadsheets";
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/spreadsheets");

    private final String credentialsFile;
    private volatile GoogleCredentials credentials;

    public GoogleSheetsApiPublisher(
            @Value("${google.sheets.credentials-file:}") String credentialsFile) {
        this.credentialsFile = credentialsFile;
    }

    @Override
    public void publish(String spreadsheetId, List<GoogleSheetsSnapshotService.SheetSnapshot> sheets)
            throws IOException {
        String accessToken = accessToken();
        RestClient restClient = createRestClient(accessToken);

        JsonNode spreadsheet = restClient.get()
                .uri("/{spreadsheetId}?fields=sheets.properties(sheetId,title)", spreadsheetId)
                .retrieve()
                .body(JsonNode.class);
        Map<String, Integer> sheetIds = readSheetIds(spreadsheet);

        List<Map<String, Object>> addSheetRequests = new ArrayList<>();
        for (GoogleSheetsSnapshotService.SheetSnapshot sheet : sheets) {
            if (!sheetIds.containsKey(sheet.title())) {
                addSheetRequests.add(Map.of(
                        "addSheet",
                        Map.of("properties", Map.of("title", sheet.title()))));
            }
        }
        if (!addSheetRequests.isEmpty()) {
            updateSpreadsheet(restClient, spreadsheetId, addSheetRequests);
            spreadsheet = restClient.get()
                    .uri("/{spreadsheetId}?fields=sheets.properties(sheetId,title)", spreadsheetId)
                    .retrieve()
                    .body(JsonNode.class);
            sheetIds = readSheetIds(spreadsheet);
        }

        List<Map<String, Object>> requests = new ArrayList<>();
        for (GoogleSheetsSnapshotService.SheetSnapshot sheet : sheets) {
            Integer sheetId = sheetIds.get(sheet.title());
            if (sheetId == null) {
                throw new IOException("Google Sheets did not create the tab: " + sheet.title());
            }
            requests.add(clearSheetRequest(sheetId));
            requests.add(writeSheetRequest(sheetId, sheet));
        }
        updateSpreadsheet(restClient, spreadsheetId, requests);
    }

    private RestClient createRestClient(String accessToken) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder()
                .baseUrl(SHEETS_API_URL)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String accessToken() throws IOException {
        GoogleCredentials currentCredentials = credentials;
        if (currentCredentials == null) {
            synchronized (this) {
                currentCredentials = credentials;
                if (currentCredentials == null) {
                    try (InputStream input = Files.newInputStream(Path.of(credentialsFile))) {
                        currentCredentials = GoogleCredentials.fromStream(input).createScoped(SCOPES);
                    }
                    credentials = currentCredentials;
                }
            }
        }
        currentCredentials.refreshIfExpired();
        var accessToken = currentCredentials.getAccessToken();
        if (accessToken == null) {
            currentCredentials.refresh();
            accessToken = currentCredentials.getAccessToken();
        }
        if (accessToken == null) {
            throw new IOException("Google service account did not return an access token");
        }
        return accessToken.getTokenValue();
    }

    private Map<String, Integer> readSheetIds(JsonNode spreadsheet) throws IOException {
        if (spreadsheet == null || !spreadsheet.has("sheets")) {
            throw new IOException("Google Sheets returned spreadsheet metadata without tabs");
        }
        Map<String, Integer> sheetIds = new HashMap<>();
        for (JsonNode sheet : spreadsheet.path("sheets")) {
            JsonNode properties = sheet.path("properties");
            if (properties.hasNonNull("title") && properties.hasNonNull("sheetId")) {
                sheetIds.put(properties.get("title").asText(), properties.get("sheetId").asInt());
            }
        }
        return sheetIds;
    }

    private Map<String, Object> clearSheetRequest(int sheetId) {
        return Map.of(
                "repeatCell",
                Map.of(
                        "range", Map.of("sheetId", sheetId, "startRowIndex", 0, "startColumnIndex", 0),
                        "cell", Map.of(),
                        "fields", "userEnteredValue"));
    }

    private Map<String, Object> writeSheetRequest(
            int sheetId,
            GoogleSheetsSnapshotService.SheetSnapshot sheet) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(sheet.headers());
        rows.addAll(sheet.rows());

        List<Map<String, Object>> rowData = rows.stream()
                .map(row -> Map.<String, Object>of(
                        "values",
                        row.stream()
                                .map(value -> Map.<String, Object>of(
                                        "userEnteredValue", Map.of("stringValue", value)))
                                .toList()))
                .toList();
        return Map.of(
                "updateCells",
                Map.of(
                        "range", Map.of("sheetId", sheetId, "startRowIndex", 0, "startColumnIndex", 0),
                        "rows", rowData,
                        "fields", "userEnteredValue"));
    }

    private void updateSpreadsheet(RestClient restClient, String spreadsheetId, List<Map<String, Object>> requests) {
        restClient.post()
                .uri("/{spreadsheetId}:batchUpdate", spreadsheetId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("requests", requests))
                .retrieve()
                .toBodilessEntity();
    }
}
