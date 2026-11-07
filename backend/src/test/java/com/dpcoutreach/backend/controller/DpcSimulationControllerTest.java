package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.CallSubmissionRequest;
import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.CallSource;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.entity.VerificationStatus;
import com.dpcoutreach.backend.service.CallRecordQueryService;
import com.dpcoutreach.backend.service.CallVerificationService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DpcSimulationControllerTest {

    private final CallVerificationService callVerificationService = mock(CallVerificationService.class);
    private final CallRecordQueryService callRecordQueryService = mock(CallRecordQueryService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DpcSimulationController(
                                callVerificationService,
                                callRecordQueryService,
                                eventPublisher))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listsOnlySimulatedCallsForRequestedDpc() throws Exception {
        when(callRecordQueryService.findAllByDpcIdAndCallSource(7L, CallSource.SIMULATION))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/admin/simulation/call-records").param("dpcId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(callRecordQueryService).findAllByDpcIdAndCallSource(7L, CallSource.SIMULATION);
    }

    @AfterEach
    void tearDown() {
        validator.destroy();
    }

    @Test
    void submitsValidatedSimulatedCallThroughCallVerification() throws Exception {
        when(callVerificationService.verifyAndSaveSimulation(any(CallSubmissionRequest.class)))
                .thenReturn(new CallRecord(
                        new Dpc("Simulation DPC", null),
                        null,
                        "919876543210",
                        Instant.parse("2026-11-08T10:00:00Z"),
                        35,
                        VerificationStatus.UNMATCHED_NUMBER,
                        CallSource.SIMULATION,
                        null));

        mockMvc.perform(post("/api/admin/simulation/call-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dpcId": 7,
                                  "phoneNumber": "919876543210",
                                  "callTimestamp": "2026-11-08T10:00:00Z",
                                  "durationSeconds": 35
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phoneNumber").value("919876543210"))
                .andExpect(jsonPath("$.verificationStatus").value("UNMATCHED_NUMBER"))
                .andExpect(jsonPath("$.callSource").value("SIMULATION"))
                .andExpect(jsonPath("$.callType").value(org.hamcrest.Matchers.nullValue()));

        verify(callVerificationService).verifyAndSaveSimulation(any(CallSubmissionRequest.class));
        verify(eventPublisher).publishEvent(any(GoogleSheetsSyncRequestedEvent.class));
    }

    @Test
    void rejectsInvalidSimulatedCall() throws Exception {
        mockMvc.perform(post("/api/admin/simulation/call-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dpcId": null,
                                  "phoneNumber": " ",
                                  "callTimestamp": "2026-11-08T10:00:00Z",
                                  "durationSeconds": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
