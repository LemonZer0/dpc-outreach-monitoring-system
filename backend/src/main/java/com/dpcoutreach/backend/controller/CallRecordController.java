package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.CallSubmissionRequest;
import com.dpcoutreach.backend.dto.CallRecordResponse;
import com.dpcoutreach.backend.dto.CallRecordMatchRequest;
import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.CallSource;
import com.dpcoutreach.backend.service.CallRecordQueryService;
import com.dpcoutreach.backend.service.CallVerificationService;
import com.dpcoutreach.backend.service.GoogleSheetsSyncRequestedEvent;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/call-records")
public class CallRecordController {

    private final CallVerificationService callVerificationService;
    private final CallRecordQueryService callRecordQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public CallRecordController(
            CallVerificationService callVerificationService,
            CallRecordQueryService callRecordQueryService,
            ApplicationEventPublisher eventPublisher) {
        this.callVerificationService = callVerificationService;
        this.callRecordQueryService = callRecordQueryService;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    public ResponseEntity<CallRecordResponse> submitCall(@Valid @RequestBody CallSubmissionRequest request) {
        CallRecord callRecord = callVerificationService.verifyAndSave(request);
        eventPublisher.publishEvent(new GoogleSheetsSyncRequestedEvent());
        return ResponseEntity.status(HttpStatus.CREATED).body(CallRecordResponse.from(callRecord));
    }

    @GetMapping
    public List<CallRecordResponse> getCallRecords(@RequestParam Long dpcId) {
        return callRecordQueryService.findAllByDpcIdAndCallSource(dpcId, CallSource.APP);
    }

    @PostMapping("/{id}/match")
    public CallRecordResponse matchUnmatchedCall(
            @PathVariable Long id,
            @Valid @RequestBody CallRecordMatchRequest request) {
        CallRecord callRecord = callVerificationService.matchUnmatchedCall(id, request);
        eventPublisher.publishEvent(new GoogleSheetsSyncRequestedEvent());
        return CallRecordResponse.from(callRecord);
    }

    @GetMapping("/{id}")
    public CallRecordResponse getCallRecord(@PathVariable Long id) {
        return callRecordQueryService.findById(id);
    }
}
