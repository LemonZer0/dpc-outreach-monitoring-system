package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.CallRecordResponse;
import com.dpcoutreach.backend.dto.CallSubmissionRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/simulation")
public class DpcSimulationController {

    private final CallVerificationService callVerificationService;
    private final CallRecordQueryService callRecordQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public DpcSimulationController(
            CallVerificationService callVerificationService,
            CallRecordQueryService callRecordQueryService,
            ApplicationEventPublisher eventPublisher) {
        this.callVerificationService = callVerificationService;
        this.callRecordQueryService = callRecordQueryService;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/call-records")
    public List<CallRecordResponse> getCallRecords(@RequestParam Long dpcId) {
        return callRecordQueryService.findAllByDpcIdAndCallSource(dpcId, CallSource.SIMULATION);
    }

    @PostMapping("/call-records")
    public ResponseEntity<CallRecordResponse> addCallRecord(
            @Valid @RequestBody CallSubmissionRequest request) {
        var callRecord = callVerificationService.verifyAndSaveSimulation(request);
        eventPublisher.publishEvent(new GoogleSheetsSyncRequestedEvent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CallRecordResponse.from(callRecord));
    }
}
