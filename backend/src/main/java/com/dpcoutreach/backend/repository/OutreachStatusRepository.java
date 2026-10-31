package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.OutreachStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OutreachStatusRepository extends JpaRepository<OutreachStatus, Long> {

    Optional<OutreachStatus> findByCompanyId(Long companyId);

    Optional<OutreachStatus> findByLatestCallRecord_Id(Long callRecordId);

    @EntityGraph(attributePaths = {"company", "latestCallRecord"})
    List<OutreachStatus> findAllByOrderByUpdatedAtDesc();

    boolean existsByCompanyId(Long companyId);
}
