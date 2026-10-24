package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.OutreachStatusResponse;
import com.dpcoutreach.backend.service.OutreachStatusQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/outreach-statuses")
public class OutreachStatusController {

    private final OutreachStatusQueryService outreachStatusQueryService;

    public OutreachStatusController(OutreachStatusQueryService outreachStatusQueryService) {
        this.outreachStatusQueryService = outreachStatusQueryService;
    }

    @GetMapping
    public List<OutreachStatusResponse> getOutreachStatuses() {
        return outreachStatusQueryService.findAll();
    }
}
