package com.dpcoutreach.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record DpcWriteRequest(@NotBlank String name) {
}
