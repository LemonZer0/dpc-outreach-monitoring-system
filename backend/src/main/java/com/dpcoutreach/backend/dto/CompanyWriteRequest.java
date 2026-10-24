package com.dpcoutreach.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanyWriteRequest(@NotBlank String name) {
}
