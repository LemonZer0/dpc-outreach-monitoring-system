package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.CompanyResponse;
import com.dpcoutreach.backend.dto.CompanyWriteRequest;
import com.dpcoutreach.backend.dto.CoreOutreachStatusResponse;
import com.dpcoutreach.backend.dto.CoreOutreachStatusUpdateRequest;
import com.dpcoutreach.backend.dto.ContactResponse;
import com.dpcoutreach.backend.dto.ContactWriteRequest;
import com.dpcoutreach.backend.dto.DpcResponse;
import com.dpcoutreach.backend.dto.DpcWriteRequest;
import com.dpcoutreach.backend.service.AdminManagementService;
import com.dpcoutreach.backend.service.CoreOutreachStatusService;
import com.dpcoutreach.backend.service.GoogleSheetsSyncRequestedEvent;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;
    private final CoreOutreachStatusService coreOutreachStatusService;
    private final ApplicationEventPublisher eventPublisher;

    public AdminManagementController(
            AdminManagementService adminManagementService,
            CoreOutreachStatusService coreOutreachStatusService,
            ApplicationEventPublisher eventPublisher) {
        this.adminManagementService = adminManagementService;
        this.coreOutreachStatusService = coreOutreachStatusService;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/companies")
    public List<CompanyResponse> getCompanies() {
        return adminManagementService.findAllCompanies();
    }

    @PostMapping("/dpcs")
    public ResponseEntity<DpcResponse> createDpc(@Valid @RequestBody DpcWriteRequest request) {
        DpcResponse response = adminManagementService.createDpc(request);
        requestSheetsSync();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/companies")
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CompanyWriteRequest request) {
        CompanyResponse response = adminManagementService.createCompany(request);
        requestSheetsSync();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/companies/{companyId}")
    public CompanyResponse updateCompany(
            @PathVariable Long companyId,
            @Valid @RequestBody CompanyWriteRequest request) {
        CompanyResponse response = adminManagementService.updateCompany(companyId, request);
        requestSheetsSync();
        return response;
    }

    @DeleteMapping("/companies/{companyId}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long companyId) {
        adminManagementService.deleteCompany(companyId);
        requestSheetsSync();
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/companies/{companyId}/outreach-status")
    public CoreOutreachStatusResponse updateCompanyOutreachStatus(
            @PathVariable Long companyId,
            @Valid @RequestBody CoreOutreachStatusUpdateRequest request) {
        CoreOutreachStatusResponse response = coreOutreachStatusService.updateStatus(companyId, request.status());
        requestSheetsSync();
        return response;
    }

    @PostMapping("/contacts")
    public ResponseEntity<ContactResponse> createContact(@Valid @RequestBody ContactWriteRequest request) {
        ContactResponse response = adminManagementService.createContact(request);
        requestSheetsSync();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/contacts/{contactId}")
    public ContactResponse updateContact(
            @PathVariable Long contactId,
            @Valid @RequestBody ContactWriteRequest request) {
        ContactResponse response = adminManagementService.updateContact(contactId, request);
        requestSheetsSync();
        return response;
    }

    @DeleteMapping("/contacts/{contactId}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long contactId) {
        adminManagementService.deleteContact(contactId);
        requestSheetsSync();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/call-records/{callRecordId}")
    public ResponseEntity<Void> deleteCallRecord(@PathVariable Long callRecordId) {
        adminManagementService.deleteCallRecord(callRecordId);
        requestSheetsSync();
        return ResponseEntity.noContent().build();
    }

    private void requestSheetsSync() {
        eventPublisher.publishEvent(new GoogleSheetsSyncRequestedEvent());
    }
}
