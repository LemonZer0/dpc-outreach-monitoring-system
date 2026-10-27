package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.dto.OutreachStatusResponse;
import com.dpcoutreach.backend.entity.CoreOutreachStatus;
import com.dpcoutreach.backend.entity.OutreachStatus;
import com.dpcoutreach.backend.repository.CompanyRepository;
import com.dpcoutreach.backend.repository.CoreOutreachStatusRepository;
import com.dpcoutreach.backend.repository.OutreachStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OutreachStatusQueryService {

    private final OutreachStatusRepository outreachStatusRepository;
    private final CoreOutreachStatusRepository coreOutreachStatusRepository;
    private final CompanyRepository companyRepository;

    public OutreachStatusQueryService(
            OutreachStatusRepository outreachStatusRepository,
            CoreOutreachStatusRepository coreOutreachStatusRepository,
            CompanyRepository companyRepository) {
        this.outreachStatusRepository = outreachStatusRepository;
        this.coreOutreachStatusRepository = coreOutreachStatusRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<OutreachStatusResponse> findAll() {
        Map<Long, OutreachStatus> callStatuses = outreachStatusRepository.findAllByOrderByUpdatedAtDesc().stream()
                .collect(Collectors.toMap(status -> status.getCompany().getId(), Function.identity()));
        Map<Long, CoreOutreachStatus> coreStatuses = coreOutreachStatusRepository
                .findAllByOrderByUpdatedAtDesc().stream()
                .collect(Collectors.toMap(status -> status.getCompany().getId(), Function.identity()));

        return companyRepository.findAllByOrderByNameAsc().stream()
                .map(company -> OutreachStatusResponse.from(
                        company,
                        callStatuses.get(company.getId()),
                        coreStatuses.get(company.getId())))
                .toList();
    }
}
