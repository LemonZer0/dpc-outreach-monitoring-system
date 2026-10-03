package com.dpcoutreach.backend.dto;

import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.VerificationStatus;

import java.time.Instant;

public record CallSubmissionResponse(
        Long id,
        Long dpcId,
        Long contactId,
        String phoneNumber,
        Instant callTimestamp,
        Integer durationSeconds,
        VerificationStatus verificationStatus) {

    public static CallSubmissionResponse from(CallRecord callRecord) {
        return new CallSubmissionResponse(
                callRecord.getId(),
                callRecord.getDpc().getId(),
                callRecord.getContact() == null ? null : callRecord.getContact().getId(),
                callRecord.getPhoneNumber(),
                callRecord.getCallTimestamp(),
                callRecord.getDurationSeconds(),
                callRecord.getVerificationStatus());
    }
}
