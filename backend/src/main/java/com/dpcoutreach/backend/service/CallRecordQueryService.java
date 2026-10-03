package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.dto.CallRecordResponse;
import com.dpcoutreach.backend.repository.CallRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CallRecordQueryService {

    private final CallRecordRepository callRecordRepository;

    public CallRecordQueryService(CallRecordRepository callRecordRepository) {
        this.callRecordRepository = callRecordRepository;
    }

    @Transactional(readOnly = true)
    public CallRecordResponse findById(Long id) {
        return callRecordRepository.findById(id)
                .map(CallRecordResponse::from)
                .orElseThrow(() -> new CallRecordNotFoundException(id));
    }
}
