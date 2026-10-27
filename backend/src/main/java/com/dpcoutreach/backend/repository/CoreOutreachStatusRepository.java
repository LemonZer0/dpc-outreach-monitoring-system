package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.CoreOutreachStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoreOutreachStatusRepository extends JpaRepository<CoreOutreachStatus, Long> {

    @EntityGraph(attributePaths = "company")
    List<CoreOutreachStatus> findAllByOrderByUpdatedAtDesc();

    Optional<CoreOutreachStatus> findByCompanyId(Long companyId);

    boolean existsByCompanyId(Long companyId);
}
