package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.CoreOutreachStatusResponse;
import com.dpcoutreach.backend.dto.DpcResponse;
import com.dpcoutreach.backend.dto.DpcWriteRequest;
import com.dpcoutreach.backend.entity.CoreOutreachStatusValue;
import com.dpcoutreach.backend.service.AdminManagementService;
import com.dpcoutreach.backend.service.CoreOutreachStatusService;
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

import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminManagementControllerTest {

    private final AdminManagementService adminManagementService = mock(AdminManagementService.class);
    private final CoreOutreachStatusService coreOutreachStatusService = mock(CoreOutreachStatusService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminManagementController(
                                adminManagementService,
                                coreOutreachStatusService,
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
    void updatesManualCompanyOutreachStatus() throws Exception {
        when(coreOutreachStatusService.updateStatus(9L, CoreOutreachStatusValue.CONFIRMED))
                .thenReturn(new CoreOutreachStatusResponse(
                        9L, CoreOutreachStatusValue.CONFIRMED, Instant.parse("2026-11-08T10:30:00Z")));

        mockMvc.perform(put("/api/admin/companies/9/outreach-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(9))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.updatedAt").value("2026-11-08T10:30:00Z"));

        verify(coreOutreachStatusService).updateStatus(9L, CoreOutreachStatusValue.CONFIRMED);
    }

    @Test
    void rejectsUnsupportedManualOutreachStatus() throws Exception {
        mockMvc.perform(put("/api/admin/companies/9/outreach-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_COMING\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsDpc() throws Exception {
        when(adminManagementService.createDpc(any(DpcWriteRequest.class)))
                .thenReturn(new DpcResponse(27L, "New DPC"));

        mockMvc.perform(post("/api/admin/dpcs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New DPC\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(27))
                .andExpect(jsonPath("$.name").value("New DPC"))
                .andExpect(jsonPath("$.accountInformation").doesNotExist());
        verify(adminManagementService).createDpc(new DpcWriteRequest("New DPC"));
        verify(eventPublisher).publishEvent(any(GoogleSheetsSyncRequestedEvent.class));
    }

    @Test
    void rejectsBlankDpcName() throws Exception {
        mockMvc.perform(post("/api/admin/dpcs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletesAnyCallRecord() throws Exception {
        mockMvc.perform(delete("/api/admin/call-records/42"))
                .andExpect(status().isNoContent());

        verify(adminManagementService).deleteCallRecord(42L);
    }

    @Test
    void deletesDpcAndRequestsSheetsSync() throws Exception {
        mockMvc.perform(delete("/api/admin/dpcs/27"))
                .andExpect(status().isNoContent());

        verify(adminManagementService).deleteDpc(27L);
        verify(eventPublisher).publishEvent(any(GoogleSheetsSyncRequestedEvent.class));
    }
}
