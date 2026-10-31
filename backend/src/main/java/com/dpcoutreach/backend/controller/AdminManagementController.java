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
import jakarta.validation.Valid;
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

    public AdminManagementController(
            AdminManagementService adminManagementService,
            CoreOutreachStatusService coreOutreachStatusService) {
        this.adminManagementService = adminManagementService;
        this.coreOutreachStatusService = coreOutreachStatusService;
    }

    @GetMapping("/companies")
    public List<CompanyResponse> getCompanies() {
        return adminManagementService.findAllCompanies();
    }

    @PostMapping("/dpcs")
    public ResponseEntity<DpcResponse> createDpc(@Valid @RequestBody DpcWriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminManagementService.createDpc(request));
    }

    @PostMapping("/companies")
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CompanyWriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminManagementService.createCompany(request));
    }

    @PutMapping("/companies/{companyId}")
    public CompanyResponse updateCompany(
            @PathVariable Long companyId,
            @Valid @RequestBody CompanyWriteRequest request) {
        return adminManagementService.updateCompany(companyId, request);
    }

    @DeleteMapping("/companies/{companyId}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long companyId) {
        adminManagementService.deleteCompany(companyId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/companies/{companyId}/outreach-status")
    public CoreOutreachStatusResponse updateCompanyOutreachStatus(
            @PathVariable Long companyId,
            @Valid @RequestBody CoreOutreachStatusUpdateRequest request) {
        return coreOutreachStatusService.updateStatus(companyId, request.status());
    }

    @PostMapping("/contacts")
    public ResponseEntity<ContactResponse> createContact(@Valid @RequestBody ContactWriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminManagementService.createContact(request));
    }

    @PutMapping("/contacts/{contactId}")
    public ContactResponse updateContact(
            @PathVariable Long contactId,
            @Valid @RequestBody ContactWriteRequest request) {
        return adminManagementService.updateContact(contactId, request);
    }

    @DeleteMapping("/contacts/{contactId}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long contactId) {
        adminManagementService.deleteContact(contactId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/call-records/{callRecordId}")
    public ResponseEntity<Void> deleteDuplicateCallRecord(@PathVariable Long callRecordId) {
        adminManagementService.deleteDuplicateCallRecord(callRecordId);
        return ResponseEntity.noContent().build();
    }
}
