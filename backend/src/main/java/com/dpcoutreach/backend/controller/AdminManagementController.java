package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.CompanyResponse;
import com.dpcoutreach.backend.dto.CompanyWriteRequest;
import com.dpcoutreach.backend.dto.ContactResponse;
import com.dpcoutreach.backend.dto.ContactWriteRequest;
import com.dpcoutreach.backend.service.AdminManagementService;
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

    public AdminManagementController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    @GetMapping("/companies")
    public List<CompanyResponse> getCompanies() {
        return adminManagementService.findAllCompanies();
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
}
