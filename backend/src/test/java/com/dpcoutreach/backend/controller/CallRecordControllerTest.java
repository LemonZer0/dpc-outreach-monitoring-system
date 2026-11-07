package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.CallRecordMatchRequest;
import com.dpcoutreach.backend.dto.CallRecordResponse;
import com.dpcoutreach.backend.dto.CallSubmissionRequest;
import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.CallSource;
import com.dpcoutreach.backend.entity.CallType;
import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.entity.VerificationStatus;
import com.dpcoutreach.backend.service.CallRecordNotFoundException;
import com.dpcoutreach.backend.service.CallRecordQueryService;
import com.dpcoutreach.backend.service.CallVerificationService;
import com.dpcoutreach.backend.service.DpcNotFoundException;
import com.dpcoutreach.backend.service.GoogleSheetsSyncRequestedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CallRecordControllerTest {

    private final CallVerificationService callVerificationService = mock(CallVerificationService.class);
    private final CallRecordQueryService callRecordQueryService = mock(CallRecordQueryService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new CallRecordController(
                                callVerificationService,
                                callRecordQueryService,
                                eventPublisher))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.destroy();
    }

    @Test
    void submitsCallAndReturnsCreatedResponseDto() throws Exception {
        when(callVerificationService.verifyAndSave(any(CallSubmissionRequest.class))).thenReturn(callRecord());

        mockMvc.perform(post("/api/call-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phoneNumber").value("919876543210"))
                .andExpect(jsonPath("$.verificationStatus").value("MATCHED_CONNECTED"))
                .andExpect(jsonPath("$.durationSeconds").value(35));

        verify(callVerificationService).verifyAndSave(any(CallSubmissionRequest.class));
        verify(eventPublisher).publishEvent(any(GoogleSheetsSyncRequestedEvent.class));
    }

    @Test
    void returnsBadRequestForInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/call-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dpcId": null,
                                  "phoneNumber": "  ",
                                  "callTimestamp": "2026-10-13T10:00:00Z",
                                  "durationSeconds": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(callVerificationService, never()).verifyAndSave(any(CallSubmissionRequest.class));
    }

    @Test
    void returnsNotFoundWhenDpcDoesNotExist() throws Exception {
        when(callVerificationService.verifyAndSave(any(CallSubmissionRequest.class)))
                .thenThrow(new DpcNotFoundException(77L));

        mockMvc.perform(post("/api/call-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("DPC with id 77 was not found"));
    }

    @Test
    void savesUnmatchedCallForAmbiguousContactMatch() throws Exception {
        when(callVerificationService.verifyAndSave(any(CallSubmissionRequest.class)))
                .thenReturn(new CallRecord(
                        new Dpc("Example DPC", "account"),
                        null,
                        "919876543210",
                        Instant.parse("2026-10-13T10:00:00Z"),
                        35,
                        VerificationStatus.UNMATCHED_NUMBER));

        mockMvc.perform(post("/api/call-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contactId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.verificationStatus").value("UNMATCHED_NUMBER"));
    }

    @Test
    void retrievesSavedCallRecord() throws Exception {
        when(callRecordQueryService.findById(42L)).thenReturn(CallRecordResponse.from(callRecord()));

        mockMvc.perform(get("/api/call-records/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("919876543210"))
                .andExpect(jsonPath("$.verificationStatus").value("MATCHED_CONNECTED"));

        verify(callRecordQueryService).findById(42L);
    }

    @Test
    void retrievesCallRecordsForDpc() throws Exception {
        when(callRecordQueryService.findAllByDpcIdAndCallSource(7L, CallSource.APP))
                .thenReturn(List.of(CallRecordResponse.from(callRecord())));

        mockMvc.perform(get("/api/call-records").param("dpcId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].phoneNumber").value("919876543210"))
                .andExpect(jsonPath("$[0].verificationStatus").value("MATCHED_CONNECTED"))
                .andExpect(jsonPath("$[0].callSource").value("APP"));

        verify(callRecordQueryService).findAllByDpcIdAndCallSource(7L, CallSource.APP);
    }

    @Test
    void matchesAnUnmatchedCallAndReturnsUpdatedRecord() throws Exception {
        when(callVerificationService.matchUnmatchedCall(42L, new CallRecordMatchRequest("New Contact", 8L)))
                .thenReturn(callRecord());

        mockMvc.perform(post("/api/call-records/42/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contactName":"New Contact","companyId":8}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("MATCHED_CONNECTED"));

        verify(callVerificationService).matchUnmatchedCall(
                42L, new CallRecordMatchRequest("New Contact", 8L));
    }

    @Test
    void rejectsMatchRequestsWithoutANameOrPositiveCompanyId() throws Exception {
        mockMvc.perform(post("/api/call-records/42/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contactName":" ","companyId":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(callVerificationService, never()).matchUnmatchedCall(
                42L, new CallRecordMatchRequest(" ", 0L));
    }

    @Test
    void returnsEmptyListWhenDpcHasNoCallRecords() throws Exception {
        when(callRecordQueryService.findAllByDpcIdAndCallSource(7L, CallSource.APP)).thenReturn(List.of());

        mockMvc.perform(get("/api/call-records").param("dpcId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void returnsNotFoundWhenCallRecordDoesNotExist() throws Exception {
        when(callRecordQueryService.findById(404L)).thenThrow(new CallRecordNotFoundException(404L));

        mockMvc.perform(get("/api/call-records/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Call record with id 404 was not found"));
    }

    private static String validRequest() {
        return """
                {
                  "dpcId": 1,
                  "phoneNumber": "+91 (98765) 43210",
                  "callTimestamp": "2026-10-13T10:00:00Z",
                  "durationSeconds": 35,
                  "callType": "DIALER"
                }
                """;
    }

    private static CallRecord callRecord() {
        Company company = new Company("Example Company");
        Contact contact = new Contact("Example Contact", "919876543210", company);
        Dpc dpc = new Dpc("Example DPC", "account");
        return new CallRecord(
                dpc, contact, "919876543210", Instant.parse("2026-10-13T10:00:00Z"),
                35, VerificationStatus.MATCHED_CONNECTED, CallSource.APP, CallType.DIALER);
    }
}
