package com.dpcoutreach.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @Test
    void reportsWhenDemoProfileIsActive() {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[] {"demo"});

        Map<String, Object> response = new HealthController(environment).health();

        assertEquals("UP", response.get("status"));
        assertEquals(true, response.get("demoMode"));
    }

    @Test
    void reportsWhenDemoProfileIsNotActive() {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});

        Map<String, Object> response = new HealthController(environment).health();

        assertEquals("UP", response.get("status"));
        assertEquals(false, response.get("demoMode"));
    }
}
