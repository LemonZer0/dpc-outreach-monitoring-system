package com.dpcoutreach.backend.dto;

import com.dpcoutreach.backend.entity.Company;

public record CompanyResponse(Long id, String name) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(company.getId(), company.getName());
    }
}
