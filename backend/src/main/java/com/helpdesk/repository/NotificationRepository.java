package com.helpdesk.repository;

import com.helpdesk.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndSeenFalse(Long userId);
    long countByUserIdAndSeenFalse(Long userId);
}
