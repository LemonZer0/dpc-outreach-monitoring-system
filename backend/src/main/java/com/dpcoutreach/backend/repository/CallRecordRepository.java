package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.VerificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {

    List<CallRecord> findAllByDpcIdOrderByCallTimestampDesc(Long dpcId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select callRecord from CallRecord callRecord where callRecord.id = :id")
    Optional<CallRecord> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<CallRecord> findAllByPhoneNumberAndContactIsNullAndVerificationStatusNot(
            String phoneNumber,
            VerificationStatus verificationStatus);

    boolean existsByDpcIdAndPhoneNumberAndCallTimestampAndDurationSeconds(
            Long dpcId,
            String phoneNumber,
            Instant callTimestamp,
            Integer durationSeconds);

    @Modifying
    @Query("""
            update CallRecord callRecord
            set callRecord.contact = null, callRecord.verificationStatus = :unmatchedStatus
            where callRecord.contact.id = :contactId
            """)
    int clearContactReference(
            @Param("contactId") Long contactId,
            @Param("unmatchedStatus") VerificationStatus unmatchedStatus);
}
