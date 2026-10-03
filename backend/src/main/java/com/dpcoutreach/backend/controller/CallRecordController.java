package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.CallSubmissionRequest;
import com.dpcoutreach.backend.dto.CallSubmissionResponse;
import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.service.CallVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/call-records")
public class CallRecordController {

    private final CallVerificationService callVerificationService;

    public CallRecordController(CallVerificationService callVerificationService) {
        this.callVerificationService = callVerificationService;
    }

    @PostMapping
    public ResponseEntity<CallSubmissionResponse> submitCall(@Valid @RequestBody CallSubmissionRequest request) {
        CallRecord callRecord = callVerificationService.verifyAndSave(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CallSubmissionResponse.from(callRecord));
    }
}
