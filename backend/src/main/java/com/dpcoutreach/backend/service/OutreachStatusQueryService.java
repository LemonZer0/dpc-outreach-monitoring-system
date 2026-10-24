package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.dto.OutreachStatusResponse;
import com.dpcoutreach.backend.repository.OutreachStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutreachStatusQueryService {

    private final OutreachStatusRepository outreachStatusRepository;

    public OutreachStatusQueryService(OutreachStatusRepository outreachStatusRepository) {
        this.outreachStatusRepository = outreachStatusRepository;
    }

    @Transactional(readOnly = true)
    public List<OutreachStatusResponse> findAll() {
        return outreachStatusRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(OutreachStatusResponse::from)
                .toList();
    }
}
