package com.dpcoutreach.backend.dto;

import com.dpcoutreach.backend.entity.CoreOutreachStatus;
import com.dpcoutreach.backend.entity.CoreOutreachStatusValue;

import java.time.Instant;

public record CoreOutreachStatusResponse(
        Long companyId,
        CoreOutreachStatusValue status,
        Instant updatedAt) {

    public static CoreOutreachStatusResponse from(CoreOutreachStatus status) {
        return new CoreOutreachStatusResponse(
                status.getCompany().getId(),
                status.getStatus(),
                status.getUpdatedAt());
    }
}
