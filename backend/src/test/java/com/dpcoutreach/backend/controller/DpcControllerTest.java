package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.DpcResponse;
import com.dpcoutreach.backend.service.DpcQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DpcControllerTest {

    private final DpcQueryService dpcQueryService = mock(DpcQueryService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DpcController(dpcQueryService)).build();
    }

    @Test
    void returnsDpcIdsAndNamesWithoutAccountInformation() throws Exception {
        when(dpcQueryService.findAll()).thenReturn(List.of(
                new DpcResponse(7L, "Alex DPC"),
                new DpcResponse(15L, "Sam DPC")));

        mockMvc.perform(get("/api/dpcs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Alex DPC"))
                .andExpect(jsonPath("$[0].accountInformation").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(15))
                .andExpect(jsonPath("$[1].name").value("Sam DPC"));

        verify(dpcQueryService).findAll();
    }

    @Test
    void returnsEmptyListWhenNoDpcsExist() throws Exception {
        when(dpcQueryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/dpcs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
