package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.dto.CoreOutreachStatusResponse;
import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.CoreOutreachStatus;
import com.dpcoutreach.backend.entity.CoreOutreachStatusValue;
import com.dpcoutreach.backend.repository.CompanyRepository;
import com.dpcoutreach.backend.repository.CoreOutreachStatusRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoreOutreachStatusServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final CoreOutreachStatusRepository coreOutreachStatusRepository =
            mock(CoreOutreachStatusRepository.class);
    private final CoreOutreachStatusService service =
            new CoreOutreachStatusService(companyRepository, coreOutreachStatusRepository);

    @Test
    void createsManualStatusForCompany() {
        Company company = new Company("Example Co");
        when(companyRepository.findById(12L)).thenReturn(Optional.of(company));
        when(coreOutreachStatusRepository.findByCompanyId(12L)).thenReturn(Optional.empty());
        when(coreOutreachStatusRepository.save(any(CoreOutreachStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CoreOutreachStatusResponse response =
                service.updateStatus(12L, CoreOutreachStatusValue.CONFIRMED);

        assertEquals(CoreOutreachStatusValue.CONFIRMED, response.status());
        assertNotNull(response.updatedAt());
        verify(coreOutreachStatusRepository).save(any(CoreOutreachStatus.class));
    }

    @Test
    void updatesExistingManualStatus() {
        Company company = new Company("Example Co");
        CoreOutreachStatus existing = new CoreOutreachStatus(
                company, CoreOutreachStatusValue.PENDING, java.time.Instant.parse("2026-01-01T00:00:00Z"));
        when(companyRepository.findById(12L)).thenReturn(Optional.of(company));
        when(coreOutreachStatusRepository.findByCompanyId(12L)).thenReturn(Optional.of(existing));
        when(coreOutreachStatusRepository.save(existing)).thenReturn(existing);

        CoreOutreachStatusResponse response =
                service.updateStatus(12L, CoreOutreachStatusValue.DECLINED);

        assertEquals(CoreOutreachStatusValue.DECLINED, response.status());
        assertNotNull(response.updatedAt());
        verify(coreOutreachStatusRepository).save(existing);
    }

    @Test
    void clearsManualStatusWhenSetToNotSet() {
        Company company = new Company("Example Co");
        CoreOutreachStatus existing = new CoreOutreachStatus(
                company, CoreOutreachStatusValue.PENDING, java.time.Instant.parse("2026-01-01T00:00:00Z"));
        when(companyRepository.findById(12L)).thenReturn(Optional.of(company));
        when(coreOutreachStatusRepository.findByCompanyId(12L)).thenReturn(Optional.of(existing));

        CoreOutreachStatusResponse response =
                service.updateStatus(12L, CoreOutreachStatusValue.NOT_SET);

        assertEquals(CoreOutreachStatusValue.NOT_SET, response.status());
        assertNull(response.updatedAt());
        verify(coreOutreachStatusRepository).delete(existing);
        verify(coreOutreachStatusRepository, never()).save(existing);
    }
}
