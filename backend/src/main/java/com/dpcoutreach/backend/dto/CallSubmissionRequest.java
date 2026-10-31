package com.dpcoutreach.backend.dto;

import com.dpcoutreach.backend.entity.CallOutcome;
import com.dpcoutreach.backend.entity.CallType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public record CallSubmissionRequest(
        @NotNull Long dpcId,
        @NotBlank String phoneNumber,
        @NotNull Instant callTimestamp,
        @NotNull @PositiveOrZero Integer durationSeconds,
        CallType callType,
        CallOutcome callOutcome) {

    public CallSubmissionRequest(
            Long dpcId,
            String phoneNumber,
            Instant callTimestamp,
            Integer durationSeconds,
            CallType callType) {
        this(dpcId, phoneNumber, callTimestamp, durationSeconds, callType, null);
    }

    public CallSubmissionRequest(
            Long dpcId,
            String phoneNumber,
            Instant callTimestamp,
            Integer durationSeconds) {
        this(dpcId, phoneNumber, callTimestamp, durationSeconds, null, null);
    }
}