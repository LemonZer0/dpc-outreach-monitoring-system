package com.dpcoutreach.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CallRecordMatchRequest(
        @NotBlank String contactName,
        @NotNull @Positive Long companyId) {
}
