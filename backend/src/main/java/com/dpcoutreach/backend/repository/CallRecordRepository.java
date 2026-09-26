package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.CallRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {

    List<CallRecord> findAllByDpcIdOrderByCallTimestampDesc(Long dpcId);
}