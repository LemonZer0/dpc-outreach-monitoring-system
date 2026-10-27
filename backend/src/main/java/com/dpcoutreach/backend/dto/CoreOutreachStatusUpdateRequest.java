package com.dpcoutreach.backend.dto;

import com.dpcoutreach.backend.entity.CoreOutreachStatusValue;
import jakarta.validation.constraints.NotNull;

public record CoreOutreachStatusUpdateRequest(
        @NotNull CoreOutreachStatusValue status) {}
