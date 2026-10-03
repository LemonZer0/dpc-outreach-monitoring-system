package com.dpcoutreach.backend.integration;

import com.dpcoutreach.backend.dto.CallSubmissionRequest;
import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.entity.OutreachStatus;
import com.dpcoutreach.backend.entity.VerificationStatus;
import com.dpcoutreach.backend.repository.CallRecordRepository;
import com.dpcoutreach.backend.repository.CompanyRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
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

    private Company company;
    private Contact contact;
    private Dpc dpc;
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
            outreachStatusRepository.findByCompanyId(company.getId()).ifPresent(outreachStatusRepository::delete);
        }
        if (dpc != null) {
            callRecordRepository.findAllByDpcIdOrderByCallTimestampDesc(dpc.getId())
                    .forEach(callRecordRepository::delete);
        }
        if (contact != null) {
            contactRepository.delete(contact);
        }
        if (company != null) {
            companyRepository.delete(company);
        }
        if (dpc != null) {
            dpcRepository.delete(dpc);
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
    }

    private CallRecord submitWhenReleased(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent submission");
        }
        return callVerificationService.verifyAndSave(request);
    }
}
