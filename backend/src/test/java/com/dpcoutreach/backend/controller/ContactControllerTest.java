package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.ContactResponse;
import com.dpcoutreach.backend.service.ContactQueryService;
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

class ContactControllerTest {

    private final ContactQueryService contactQueryService = mock(ContactQueryService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ContactController(contactQueryService)).build();
    }

    @Test
    void returnsContactsWithTheirCompanyDetails() throws Exception {
        when(contactQueryService.findAll())
                .thenReturn(List.of(new ContactResponse(5L, "Alex Recruiter", "+91 98765 43210", 9L, "Example Co")));

        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].name").value("Alex Recruiter"))
                .andExpect(jsonPath("$[0].phoneNumber").value("+91 98765 43210"))
                .andExpect(jsonPath("$[0].companyId").value(9))
                .andExpect(jsonPath("$[0].companyName").value("Example Co"));

        verify(contactQueryService).findAll();
    }

    @Test
    void returnsEmptyListWhenNoContactsExist() throws Exception {
        when(contactQueryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
