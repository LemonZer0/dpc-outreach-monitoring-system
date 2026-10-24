package com.dpcoutreach.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ContactWriteRequest(
        @NotBlank String name,
        @NotBlank String phoneNumber,
        @NotNull @Positive Long companyId) {
}
