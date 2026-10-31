package com.dpcoutreach.backend.dto;

import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.CallSource;
import com.dpcoutreach.backend.entity.CallType;
import com.dpcoutreach.backend.entity.VerificationStatus;

import java.time.Instant;

public record CallRecordResponse(
        Long id,
        Long dpcId,
        Long contactId,
        String phoneNumber,
        Instant callTimestamp,
        Integer durationSeconds,
        VerificationStatus verificationStatus,
        CallSource callSource,
        CallType callType) {

    public static CallRecordResponse from(CallRecord callRecord) {
        return new CallRecordResponse(
                callRecord.getId(),
                callRecord.getDpc().getId(),
                callRecord.getContact() == null ? null : callRecord.getContact().getId(),
                callRecord.getPhoneNumber(),
                callRecord.getCallTimestamp(),
                callRecord.getDurationSeconds(),
                callRecord.getVerificationStatus(),
                callRecord.getCallSource(),
                callRecord.getCallType());
    }
}
