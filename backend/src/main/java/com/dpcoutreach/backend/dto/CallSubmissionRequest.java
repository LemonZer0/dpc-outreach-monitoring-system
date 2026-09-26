package com.dpcoutreach.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public record CallSubmissionRequest(
        @NotNull Long dpcId,
        @NotBlank String phoneNumber,
        @NotNull Instant callTimestamp,
        @NotNull @PositiveOrZero Integer durationSeconds) {
}