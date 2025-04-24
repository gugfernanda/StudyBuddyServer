package com.example.studybuddy.repository;

import com.example.studybuddy.repository.entity.Notification;
import com.example.studybuddy.repository.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    long countByUserAndReadIsFalse(User user);
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
