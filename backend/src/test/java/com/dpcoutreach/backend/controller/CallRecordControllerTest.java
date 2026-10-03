package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.CallSubmissionRequest;
import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.entity.VerificationStatus;
import com.dpcoutreach.backend.service.AmbiguousContactMatchException;
import com.dpcoutreach.backend.service.CallVerificationService;
import com.dpcoutreach.backend.service.DpcNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CallRecordControllerTest {

    private final CallVerificationService callVerificationService = mock(CallVerificationService.class);
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CallRecordController(callVerificationService))
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
    void returnsConflictForAmbiguousContactMatch() throws Exception {
        when(callVerificationService.verifyAndSave(any(CallSubmissionRequest.class)))
                .thenThrow(new AmbiguousContactMatchException("919876543210"));

        mockMvc.perform(post("/api/call-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    private static String validRequest() {
        return """
                {
                  "dpcId": 1,
                  "phoneNumber": "+91 (98765) 43210",
                  "callTimestamp": "2026-10-13T10:00:00Z",
                  "durationSeconds": 35
                }
                """;
    }

    private static CallRecord callRecord() {
        Company company = new Company("Example Company");
        Contact contact = new Contact("Example Contact", "919876543210", company);
        Dpc dpc = new Dpc("Example DPC", "account");
        return new CallRecord(
                dpc, contact, "919876543210", Instant.parse("2026-10-13T10:00:00Z"),
                35, VerificationStatus.MATCHED_CONNECTED);
    }
}
