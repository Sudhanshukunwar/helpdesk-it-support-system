package com.helpdesk.controller;

import com.helpdesk.dto.Dtos.SlaRequest;
import com.helpdesk.entity.Priority;
import com.helpdesk.entity.SlaPolicy;
import com.helpdesk.repository.SlaPolicyRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/sla")
@RequiredArgsConstructor
public class SlaController {
    private final SlaPolicyRepository repo;

    @GetMapping
    public List<SlaPolicy> all() {
        return repo.findAll().stream().sorted(Comparator.comparing(SlaPolicy::getPriority)).toList();
    }

    @PutMapping("/{priority}")
    @PreAuthorize("hasRole('ADMIN')")
    public SlaPolicy update(@PathVariable Priority priority, @Valid @RequestBody SlaRequest r) {
        return repo.save(new SlaPolicy(priority, r.responseHours(), r.resolutionHours()));
    }
}
