package com.helpdesk.controller;

import com.helpdesk.dto.Dtos.NotificationResponse;
import com.helpdesk.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService svc;

    @GetMapping
    public List<NotificationResponse> list(Authentication a) { return svc.list(a.getName()); }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(Authentication a) { return Map.of("count", svc.unreadCount(a.getName())); }

    @PutMapping("/{id}/read")
    public void read(@PathVariable Long id, Authentication a) { svc.markRead(id, a.getName()); }

    @PutMapping("/read-all")
    public void readAll(Authentication a) { svc.markAllRead(a.getName()); }
}
