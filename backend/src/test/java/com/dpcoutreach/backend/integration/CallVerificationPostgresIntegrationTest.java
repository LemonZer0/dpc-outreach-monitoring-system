package com.dpcoutreach.backend.integration;

import com.dpcoutreach.backend.dto.CallSubmissionRequest;
import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.entity.CoreOutreachStatus;
import com.dpcoutreach.backend.entity.CoreOutreachStatusValue;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.entity.OutreachStatus;
import com.dpcoutreach.backend.entity.VerificationStatus;
import com.dpcoutreach.backend.repository.CallRecordRepository;
import com.dpcoutreach.backend.repository.CompanyRepository;
import com.dpcoutreach.backend.repository.CoreOutreachStatusRepository;
import com.dpcoutreach.backend.repository.ContactRepository;
import com.dpcoutreach.backend.repository.DpcRepository;
import com.dpcoutreach.backend.repository.OutreachStatusRepository;
import com.dpcoutreach.backend.service.CallVerificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ADMIN_USERNAME=integration-admin",
        "ADMIN_PASSWORD=integration-password"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class CallVerificationPostgresIntegrationTest {

    @Autowired
    private CallVerificationService callVerificationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private DpcRepository dpcRepository;

    @Autowired
    private CallRecordRepository callRecordRepository;

    @Autowired
    private OutreachStatusRepository outreachStatusRepository;

    @Autowired
    private CoreOutreachStatusRepository coreOutreachStatusRepository;

    private Company company;
    private Contact contact;
    private Dpc dpc;
    private Dpc createdAdminDpc;
    private CallSubmissionRequest request;

    @BeforeEach
    void createDatabaseFixtures() {
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "");
        String nationalNumber = ThreadLocalRandom.current().nextInt(6, 10)
                + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
        String formattedPhone = "+91 (" + nationalNumber.substring(0, 5) + ") "
                + nationalNumber.substring(5);

        company = companyRepository.save(new Company("Concurrent test company " + uniqueSuffix));
        dpc = dpcRepository.save(new Dpc("Concurrent test DPC " + uniqueSuffix, "integration-test"));
        contact = contactRepository.save(new Contact("Concurrent test contact", formattedPhone, company));
        request = new CallSubmissionRequest(
                dpc.getId(), formattedPhone, Instant.now().minusSeconds(2), 35);
    }

    @AfterEach
    void removeDatabaseFixtures() {
        if (company != null) {
            coreOutreachStatusRepository.findByCompanyId(company.getId())
                    .ifPresent(coreOutreachStatusRepository::delete);
            outreachStatusRepository.findByCompanyId(company.getId()).ifPresent(outreachStatusRepository::delete);
        }
        if (dpc != null) {
            callRecordRepository.findAllByDpcIdOrderByCallTimestampDesc(dpc.getId())
                    .forEach(callRecordRepository::delete);
        }
        if (contact != null) {
            contactRepository.findAllByCompany_Id(company.getId()).forEach(contactRepository::delete);
        }
        if (company != null) {
            companyRepository.delete(company);
        }
        if (dpc != null) {
            dpcRepository.delete(dpc);
        }
        if (createdAdminDpc != null) {
            dpcRepository.delete(createdAdminDpc);
        }
    }

    @Test
    void concurrentIdenticalSubmissionsSaveOneCallAndOneDuplicate() throws Exception {
        int concurrentSubmissions = 2;
        CountDownLatch ready = new CountDownLatch(concurrentSubmissions);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentSubmissions);

        try {
            Future<CallRecord> first = executor.submit(() -> submitWhenReleased(ready, start));
            Future<CallRecord> second = executor.submit(() -> submitWhenReleased(ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS), "Both submissions should be ready");
            start.countDown();

            CallRecord firstResult = first.get(15, TimeUnit.SECONDS);
            CallRecord secondResult = second.get(15, TimeUnit.SECONDS);
            List<CallRecord> savedRecords = callRecordRepository.findAllByDpcIdOrderByCallTimestampDesc(dpc.getId());

            List<VerificationStatus> resultStatuses = List.of(
                    firstResult.getVerificationStatus(), secondResult.getVerificationStatus());
            assertEquals(1, resultStatuses.stream()
                    .filter(status -> status == VerificationStatus.MATCHED_CONNECTED)
                    .count());
            assertEquals(1, resultStatuses.stream()
                    .filter(status -> status == VerificationStatus.DUPLICATE_CALL)
                    .count());
            assertEquals(2, savedRecords.size());
            assertEquals(1, savedRecords.stream()
                    .filter(record -> record.getVerificationStatus() == VerificationStatus.MATCHED_CONNECTED)
                    .count());
            assertEquals(1, savedRecords.stream()
                    .filter(record -> record.getVerificationStatus() == VerificationStatus.DUPLICATE_CALL)
                    .count());

            OutreachStatus status = outreachStatusRepository.findByCompanyId(company.getId()).orElseThrow();
            assertEquals(VerificationStatus.MATCHED_CONNECTED.name(), status.getStatus());
            assertNotNull(status.getLatestCallRecord());
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void submittedCallCanBeRetrievedThroughTheApi() throws Exception {
        String requestBody = """
                {
                  "dpcId": %d,
                  "phoneNumber": "%s",
                  "callTimestamp": "%s",
                  "durationSeconds": 35
                }
                """.formatted(dpc.getId(), request.phoneNumber(), request.callTimestamp());

        String submissionResponse = mockMvc.perform(post("/api/call-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.verificationStatus").value("MATCHED_CONNECTED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode submissionJson = objectMapper.readTree(submissionResponse);
        assertTrue(submissionJson.has("id"), "POST response should include the saved call record ID");
        long savedRecordId = submissionJson.get("id").asLong();

        mockMvc.perform(get("/api/call-records/" + savedRecordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedRecordId))
                .andExpect(jsonPath("$.phoneNumber").value(request.phoneNumber().replaceAll("\\D", "")))
                .andExpect(jsonPath("$.verificationStatus").value("MATCHED_CONNECTED"));

        mockMvc.perform(get("/api/call-records").param("dpcId", dpc.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(savedRecordId))
                .andExpect(jsonPath("$[0].verificationStatus").value("MATCHED_CONNECTED"));
    }

    @Test
    void listsKnownContactsWithCompanyDetailsThroughTheApi() throws Exception {
        JsonNode contacts = objectMapper.readTree(
                mockMvc.perform(get("/api/contacts"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        boolean foundFixtureContact = false;
        for (JsonNode listedContact : contacts) {
            if (listedContact.get("id").asLong() == contact.getId()) {
                assertEquals(contact.getName(), listedContact.get("name").asText());
                assertEquals(contact.getPhoneNumber(), listedContact.get("phoneNumber").asText());
                assertEquals(company.getId(), listedContact.get("companyId").asLong());
                assertEquals(company.getName(), listedContact.get("companyName").asText());
                foundFixtureContact = true;
                break;
            }
        }
        assertTrue(foundFixtureContact, "The contact list should include the test contact");
    }

    @Test
    void adminRoutesRequireCredentialsWhileContactDirectoryRemainsPublic() throws Exception {
        mockMvc.perform(get("/api/admin/companies"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/dpcs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unauthorized DPC\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/admin/companies/" + company.getId() + "/outreach-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/companies").header("Authorization", "Basic invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateDpcWithoutExposingAccountInformation() throws Exception {
        String name = "Created DPC " + UUID.randomUUID();
        String response = mockMvc.perform(post("/api/admin/dpcs")
                        .header("Authorization", adminAuthorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.accountInformation").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long createdId = objectMapper.readTree(response).get("id").asLong();
        createdAdminDpc = dpcRepository.findById(createdId).orElseThrow();
        assertEquals(name, createdAdminDpc.getName());
        assertNull(createdAdminDpc.getAccountInformation());
    }

    @Test
    void localDashboardCanPreflightAuthenticatedStatusUpdate() throws Exception {
        mockMvc.perform(options("/api/admin/companies/" + company.getId() + "/outreach-status")
                        .header("Origin", "http://localhost:8000")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8000"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("PUT")));
    }

    @Test
    void localDashboardCanPreflightAdminDirectoryChanges() throws Exception {
        for (var route : List.of(
                Map.entry("/api/admin/dpcs", "POST"),
                Map.entry("/api/admin/companies", "DELETE"),
                Map.entry("/api/admin/call-records/42", "DELETE"))) {
            mockMvc.perform(options(route.getKey())
                            .header("Origin", "http://localhost:8000")
                            .header("Access-Control-Request-Method", route.getValue())
                            .header("Access-Control-Request-Headers", "authorization,content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8000"))
                    .andExpect(header().string("Access-Control-Allow-Methods",
                            org.hamcrest.Matchers.containsString(route.getValue())));
        }
    }

    @Test
    void coreCanSetManualOutreachStatusSeparatelyFromCallVerification() throws Exception {
        assertEquals("NOT_SET", outreachStatusForCompany(company.getId()).get("coreOutreachStatus").asText());
        callVerificationService.verifyAndSave(request);

        mockMvc.perform(put("/api/admin/companies/" + company.getId() + "/outreach-status")
                        .header("Authorization", adminAuthorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(company.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        CoreOutreachStatus savedStatus = coreOutreachStatusRepository.findByCompanyId(company.getId())
                .orElseThrow();
        assertEquals(CoreOutreachStatusValue.PENDING, savedStatus.getStatus());
        JsonNode updatedCompanyStatus = outreachStatusForCompany(company.getId());
        assertEquals("PENDING", updatedCompanyStatus.get("coreOutreachStatus").asText());
        assertEquals("MATCHED_CONNECTED", updatedCompanyStatus.get("status").asText());

        mockMvc.perform(put("/api/admin/companies/" + company.getId() + "/outreach-status")
                        .header("Authorization", adminAuthorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/admin/companies/" + company.getId() + "/outreach-status")
                        .header("Authorization", adminAuthorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_SET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_SET"))
                .andExpect(jsonPath("$.updatedAt").value(org.hamcrest.Matchers.nullValue()));
        assertTrue(coreOutreachStatusRepository.findByCompanyId(company.getId()).isEmpty());
        assertEquals("MATCHED_CONNECTED", outreachStatusForCompany(company.getId()).get("status").asText());
    }

    private JsonNode outreachStatusForCompany(Long companyId) throws Exception {
        JsonNode statuses = objectMapper.readTree(mockMvc.perform(get("/api/outreach-statuses"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        for (JsonNode status : statuses) {
            if (status.get("companyId").asLong() == companyId) {
                return status;
            }
        }
        throw new AssertionError("Outreach status was not returned for company " + companyId);
    }

    @Test
    void adminCanCreateUpdateAndDeleteContactsAndCompanies() throws Exception {
        String companyResponse = mockMvc.perform(post("/api/admin/companies")
                        .header("Authorization", adminAuthorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Admin API test company"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Admin API test company"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long companyId = objectMapper.readTree(companyResponse).get("id").asLong();

        String contactResponse = mockMvc.perform(post("/api/admin/contacts")
                        .header("Authorization", adminAuthorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Admin API test contact","phoneNumber":"+91 (98765) 01234","companyId":%d}
                                """.formatted(companyId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Admin API test contact"))
                .andExpect(jsonPath("$.phoneNumber").value("919876501234"))
                .andExpect(jsonPath("$.companyId").value(companyId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long contactId = objectMapper.readTree(contactResponse).get("id").asLong();

        mockMvc.perform(put("/api/admin/companies/" + companyId)
                        .header("Authorization", adminAuthorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed admin test company"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed admin test company"));

        mockMvc.perform(put("/api/admin/contacts/" + contactId)
                        .header("Authorization", adminAuthorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed admin test contact","phoneNumber":"+91 90000 11111","companyId":%d}
                                """.formatted(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed admin test contact"))
                .andExpect(jsonPath("$.phoneNumber").value("919000011111"))
                .andExpect(jsonPath("$.companyName").value("Renamed admin test company"));

        mockMvc.perform(delete("/api/admin/contacts/" + contactId)
                        .header("Authorization", adminAuthorizationHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/admin/companies/" + companyId)
                        .header("Authorization", adminAuthorizationHeader()))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanDeleteContactWithCallHistoryAndKeepTheCallRecord() throws Exception {
        CallRecord savedCall = callVerificationService.verifyAndSave(request);

        mockMvc.perform(delete("/api/admin/contacts/" + contact.getId())
                        .header("Authorization", adminAuthorizationHeader()))
                .andExpect(status().isNoContent());

        assertTrue(contactRepository.findById(contact.getId()).isEmpty());
        CallRecord preservedCall = callRecordRepository.findById(savedCall.getId()).orElseThrow();
        assertNull(preservedCall.getContact());
        assertEquals(VerificationStatus.UNMATCHED_NUMBER, preservedCall.getVerificationStatus());
    }

    @Test
    void adminCanDeleteCompanyWithContactsAndHistoryAndKeepTheCallRecord() throws Exception {
        CallRecord savedCall = callVerificationService.verifyAndSave(request);
        coreOutreachStatusRepository.save(new CoreOutreachStatus(
                company, CoreOutreachStatusValue.PENDING, Instant.now()));

        mockMvc.perform(delete("/api/admin/companies/" + company.getId())
                        .header("Authorization", adminAuthorizationHeader()))
                .andExpect(status().isNoContent());

        assertTrue(companyRepository.findById(company.getId()).isEmpty());
        assertTrue(contactRepository.findById(contact.getId()).isEmpty());
        assertTrue(outreachStatusRepository.findByCompanyId(company.getId()).isEmpty());
        assertTrue(coreOutreachStatusRepository.findByCompanyId(company.getId()).isEmpty());
        CallRecord preservedCall = callRecordRepository.findById(savedCall.getId()).orElseThrow();
        assertNull(preservedCall.getContact());
        assertEquals(VerificationStatus.UNMATCHED_NUMBER, preservedCall.getVerificationStatus());
    }

    @Test
    void adminCanDeleteDuplicateCallButNotRegularCall() throws Exception {
        CallRecord duplicate = callRecordRepository.save(new CallRecord(
                dpc, null, request.phoneNumber(), Instant.now(), 10, VerificationStatus.DUPLICATE_CALL));
        CallRecord regularCall = callRecordRepository.save(new CallRecord(
                dpc, null, request.phoneNumber(), Instant.now().plusSeconds(1), 10,
                VerificationStatus.UNMATCHED_NUMBER));

        mockMvc.perform(delete("/api/admin/call-records/" + duplicate.getId()))
                .andExpect(status().isUnauthorized());
        assertTrue(callRecordRepository.findById(duplicate.getId()).isPresent());

        mockMvc.perform(delete("/api/admin/call-records/" + duplicate.getId())
                        .header("Authorization", adminAuthorizationHeader()))
                .andExpect(status().isNoContent());
        assertTrue(callRecordRepository.findById(duplicate.getId()).isEmpty());

        mockMvc.perform(delete("/api/admin/call-records/" + regularCall.getId())
                        .header("Authorization", adminAuthorizationHeader()))
                .andExpect(status().isConflict());
        assertTrue(callRecordRepository.findById(regularCall.getId()).isPresent());
    }

    @Test
    void rematchesOrphanedCallWithLegacyMatchedStatusAndCreatesContactForSelectedCompany() throws Exception {
        String unmatchedPhone = "+91 7" + String.format("%09d", ThreadLocalRandom.current().nextLong(1_000_000_000L));
        String callResponse = mockMvc.perform(post("/api/call-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dpcId": %d,
                                  "phoneNumber": "%s",
                                  "callTimestamp": "%s",
                                  "durationSeconds": 75
                                }
                                """.formatted(dpc.getId(), unmatchedPhone, Instant.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contactId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.verificationStatus").value("UNMATCHED_NUMBER"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long callId = objectMapper.readTree(callResponse).get("id").asLong();
        CallRecord orphanedCall = callRecordRepository.findById(callId).orElseThrow();
        orphanedCall.setVerificationStatus(VerificationStatus.MATCHED_CONNECTED);
        callRecordRepository.saveAndFlush(orphanedCall);
        Instant earlierCallTime = Instant.now().minusSeconds(10);
        String earlierCallResponse = mockMvc.perform(post("/api/call-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dpcId": %d,
                                  "phoneNumber": "%s",
                                  "callTimestamp": "%s",
                                  "durationSeconds": 25
                                }
                                """.formatted(dpc.getId(), unmatchedPhone, earlierCallTime)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationStatus").value("UNMATCHED_NUMBER"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long earlierCallId = objectMapper.readTree(earlierCallResponse).get("id").asLong();
        Contact existingContact = contactRepository.save(
                new Contact("Existing contact with same number", unmatchedPhone, company));

        String matchResponse = mockMvc.perform(post("/api/call-records/" + callId + "/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contactName":"Manually matched contact","companyId":%d}
                                """.formatted(company.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("MATCHED_LONG_CALL"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long contactId = objectMapper.readTree(matchResponse).get("contactId").asLong();

        Contact matchedContact = contactRepository.findById(contactId).orElseThrow();
        assertEquals("Manually matched contact", matchedContact.getName());
        assertTrue(!matchedContact.getId().equals(existingContact.getId()));
        assertEquals(company.getId(), matchedContact.getCompany().getId());
        assertEquals(unmatchedPhone.replaceAll("[^0-9]", ""), matchedContact.getPhoneNumber());
        CallRecord matchedCall = callRecordRepository.findById(callId).orElseThrow();
        assertEquals(contactId, matchedCall.getContact().getId());
        assertEquals(VerificationStatus.MATCHED_LONG_CALL, matchedCall.getVerificationStatus());
        CallRecord earlierMatchedCall = callRecordRepository.findById(earlierCallId).orElseThrow();
        assertEquals(contactId, earlierMatchedCall.getContact().getId());
        assertEquals(VerificationStatus.MATCHED_CONNECTED, earlierMatchedCall.getVerificationStatus());
        assertEquals(VerificationStatus.MATCHED_LONG_CALL.name(),
                outreachStatusRepository.findByCompanyId(company.getId()).orElseThrow().getStatus());
    }

    @Test
    void submitsLongCallThroughTheApi() throws Exception {
        String requestBody = """
                {
                  "dpcId": %d,
                  "phoneNumber": "%s",
                  "callTimestamp": "%s",
                  "durationSeconds": 61
                }
                """.formatted(dpc.getId(), request.phoneNumber(), request.callTimestamp());

        mockMvc.perform(post("/api/call-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationStatus").value("MATCHED_LONG_CALL"));

        OutreachStatus status = outreachStatusRepository.findByCompanyId(company.getId()).orElseThrow();
        assertEquals(VerificationStatus.MATCHED_LONG_CALL.name(), status.getStatus());
    }

    private CallRecord submitWhenReleased(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent submission");
        }
        return callVerificationService.verifyAndSave(request);
    }

    private static String adminAuthorizationHeader() {
        String credentials = "integration-admin:integration-password";
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
