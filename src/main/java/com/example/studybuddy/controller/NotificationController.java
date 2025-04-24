package com.example.studybuddy.controller;

import com.example.studybuddy.repository.NotificationRepository;
import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.entity.Notification;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    public NotificationController(UserRepository userRepository, NotificationService notificationService, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<?> getNotifications(@RequestParam Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(userOpt.get());
            return ResponseEntity.ok(notificationService.toDTOList(notifications));
        } else {
            return ResponseEntity.badRequest().body("User not found");
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@RequestParam Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(notificationService.countUnread(userOpt.get()));
        } else {
            return ResponseEntity.badRequest().body("User not found");
        }
    }

    @PostMapping("/mark-read")
    public ResponseEntity<?> markRead(@RequestParam Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            notificationService.markAllAsRead(userOpt.get());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("User not found");
        }
    }
}
