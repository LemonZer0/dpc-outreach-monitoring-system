package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.OutreachStatusResponse;
import com.dpcoutreach.backend.entity.CoreOutreachStatusValue;
import com.dpcoutreach.backend.service.OutreachStatusQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OutreachStatusControllerTest {

    private final OutreachStatusQueryService outreachStatusQueryService = mock(OutreachStatusQueryService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new OutreachStatusController(outreachStatusQueryService)).build();
    }

    @Test
    void returnsCompanyOutreachStatuses() throws Exception {
        when(outreachStatusQueryService.findAll()).thenReturn(List.of(new OutreachStatusResponse(
                9L,
                "Example Co",
                "MATCHED_LONG_CALL",
                Instant.parse("2026-11-08T10:30:00Z"),
                Instant.parse("2026-11-08T10:31:00Z"),
                CoreOutreachStatusValue.PENDING,
                Instant.parse("2026-11-08T10:32:00Z"))));

        mockMvc.perform(get("/api/outreach-statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyId").value(9))
                .andExpect(jsonPath("$[0].companyName").value("Example Co"))
                .andExpect(jsonPath("$[0].status").value("MATCHED_LONG_CALL"))
                .andExpect(jsonPath("$[0].coreOutreachStatus").value("PENDING"))
                .andExpect(jsonPath("$[0].latestCallTimestamp").value("2026-11-08T10:30:00Z"));

        verify(outreachStatusQueryService).findAll();
    }

    @Test
    void returnsEmptyListWhenNoCompanyHasOutreachStatus() throws Exception {
        when(outreachStatusQueryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/outreach-statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
