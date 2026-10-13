
package com.dpcoutreach.backend.controller;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

@RestController
public class HealthController {

    private final Environment environment;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        boolean demoMode = Arrays.asList(environment.getActiveProfiles()).contains("demo");
        return Map.of("status", "UP", "demoMode", demoMode);
    }
}
