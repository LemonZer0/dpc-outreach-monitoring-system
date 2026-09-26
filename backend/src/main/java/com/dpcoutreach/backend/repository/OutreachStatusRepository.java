package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.OutreachStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OutreachStatusRepository extends JpaRepository<OutreachStatus, Long> {

    Optional<OutreachStatus> findByCompanyId(Long companyId);
}