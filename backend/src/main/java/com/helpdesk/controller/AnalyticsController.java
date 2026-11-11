package com.helpdesk.controller;

import com.helpdesk.dto.Dtos.AnalyticsSummary;
import com.helpdesk.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService svc;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public AnalyticsSummary summary() { return svc.summary(); }
}
