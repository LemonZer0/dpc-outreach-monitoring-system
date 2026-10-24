package com.dpcoutreach.backend.dto;

import com.dpcoutreach.backend.entity.OutreachStatus;

import java.time.Instant;

public record OutreachStatusResponse(
        Long companyId,
        String companyName,
        String status,
        Instant latestCallTimestamp,
        Instant updatedAt) {

    public static OutreachStatusResponse from(OutreachStatus outreachStatus) {
        return new OutreachStatusResponse(
                outreachStatus.getCompany().getId(),
                outreachStatus.getCompany().getName(),
                outreachStatus.getStatus(),
                outreachStatus.getLatestCallRecord() == null
                        ? null
                        : outreachStatus.getLatestCallRecord().getCallTimestamp(),
                outreachStatus.getUpdatedAt());
    }
}
