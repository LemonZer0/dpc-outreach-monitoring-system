package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.dto.CoreOutreachStatusResponse;
import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.CoreOutreachStatus;
import com.dpcoutreach.backend.entity.CoreOutreachStatusValue;
import com.dpcoutreach.backend.repository.CompanyRepository;
import com.dpcoutreach.backend.repository.CoreOutreachStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CoreOutreachStatusService {

    private final CompanyRepository companyRepository;
    private final CoreOutreachStatusRepository coreOutreachStatusRepository;

    public CoreOutreachStatusService(
            CompanyRepository companyRepository,
            CoreOutreachStatusRepository coreOutreachStatusRepository) {
        this.companyRepository = companyRepository;
        this.coreOutreachStatusRepository = coreOutreachStatusRepository;
    }

    @Transactional
    public CoreOutreachStatusResponse updateStatus(Long companyId, CoreOutreachStatusValue statusValue) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Company", companyId));
        if (statusValue == CoreOutreachStatusValue.NOT_SET) {
            coreOutreachStatusRepository.findByCompanyId(companyId)
                    .ifPresent(coreOutreachStatusRepository::delete);
            return new CoreOutreachStatusResponse(companyId, CoreOutreachStatusValue.NOT_SET, null);
        }

        Instant updatedAt = Instant.now();
        CoreOutreachStatus status = coreOutreachStatusRepository.findByCompanyId(companyId)
                .orElseGet(() -> new CoreOutreachStatus(company, statusValue, updatedAt));
        status.setStatus(statusValue);
        status.setUpdatedAt(updatedAt);
        return CoreOutreachStatusResponse.from(coreOutreachStatusRepository.save(status));
    }
}
