package com.helpdesk.controller;

import com.helpdesk.dto.Dtos.*;
import com.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService svc;

    @PostMapping
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest r, Authentication a) {
        return svc.create(r, a.getName());
    }

    @GetMapping
    public List<TicketResponse> list(Authentication a) { return svc.list(a.getName()); }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id, Authentication a) { return svc.get(id, a.getName()); }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest r, Authentication a) {
        return svc.assign(id, r.assigneeId(), a.getName());
    }

    @PutMapping("/{id}/claim")
    @PreAuthorize("hasRole('SUPPORT')")
    public TicketResponse claim(@PathVariable Long id, Authentication a) { return svc.claim(id, a.getName()); }

    @PutMapping("/{id}/status")
    public TicketResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest r, Authentication a) {
        return svc.updateStatus(id, r.status(), a.getName());
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id, Authentication a) {
        return svc.comments(id, a.getName());
    }

    @PostMapping("/{id}/comments")
    public CommentResponse addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest r, Authentication a) {
        return svc.addComment(id, r.message(), a.getName());
    }
}
