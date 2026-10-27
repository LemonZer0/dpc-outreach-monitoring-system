package com.dpcoutreach.backend.dto;

import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.CoreOutreachStatus;
import com.dpcoutreach.backend.entity.CoreOutreachStatusValue;
import com.dpcoutreach.backend.entity.OutreachStatus;

import java.time.Instant;

public record OutreachStatusResponse(
        Long companyId,
        String companyName,
        String status,
        Instant latestCallTimestamp,
        Instant updatedAt,
        CoreOutreachStatusValue coreOutreachStatus,
        Instant coreOutreachStatusUpdatedAt) {

    public static OutreachStatusResponse from(
            Company company,
            OutreachStatus outreachStatus,
            CoreOutreachStatus coreOutreachStatus) {
        return new OutreachStatusResponse(
                company.getId(),
                company.getName(),
                outreachStatus == null ? null : outreachStatus.getStatus(),
                outreachStatus == null || outreachStatus.getLatestCallRecord() == null
                        ? null
                        : outreachStatus.getLatestCallRecord().getCallTimestamp(),
                outreachStatus == null ? null : outreachStatus.getUpdatedAt(),
                coreOutreachStatus == null ? CoreOutreachStatusValue.NOT_SET : coreOutreachStatus.getStatus(),
                coreOutreachStatus == null ? null : coreOutreachStatus.getUpdatedAt());
    }
}
