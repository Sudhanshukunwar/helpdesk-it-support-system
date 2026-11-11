package com.helpdesk.dto;

import com.helpdesk.entity.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class Dtos {
    public record CreateTicketRequest(@NotBlank String title, String description, String category,
                                      @NotNull Priority priority) {}
    public record StatusRequest(@NotNull TicketStatus status) {}
    public record AssignRequest(@NotNull Long assigneeId) {}
    public record CommentRequest(@NotBlank String message) {}

    public record TicketResponse(Long id, String title, String description, String category,
                                 TicketStatus status, Priority priority,
                                 Long createdById, String createdByName,
                                 Long assignedToId, String assignedToName,
                                 LocalDateTime createdAt, LocalDateTime dueAt, LocalDateTime resolvedAt,
                                 boolean slaBreached) {}

    public record CommentResponse(Long id, Long userId, String userName, Role userRole,
                                  String message, LocalDateTime createdAt) {}

    public record NotificationResponse(Long id, String message, Long ticketId, boolean seen,
                                       LocalDateTime createdAt) {}

    public record UserResponse(Long id, String name, String email, Role role, boolean active) {}
    public record UpdateUserRequest(Role role, Boolean active) {}

    public record SlaRequest(@Min(1) int responseHours, @Min(1) int resolutionHours) {}

    public record AgentLoad(String name, long active, long resolved) {}
    public record AnalyticsSummary(long total, Map<String, Long> byStatus, Map<String, Long> byPriority,
                                   double avgResolutionHours, double slaBreachPercent,
                                   List<AgentLoad> agentLoad) {}

    public record HistoryResponse(Long id, HistoryAction action, String details, String actorName,
                                  LocalDateTime createdAt) {}
}
