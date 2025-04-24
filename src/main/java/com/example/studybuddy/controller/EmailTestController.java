package com.example.studybuddy.controller;

import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.service.DailyEmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mail")
public class EmailTestController {

    private final UserRepository userRepository;
    private final DailyEmailService dailyEmailService;

    public EmailTestController(UserRepository userRepository, DailyEmailService dailyEmailService) {
        this.userRepository = userRepository;
        this.dailyEmailService = dailyEmailService;
    }

    @GetMapping("/daily-summary")
    public ResponseEntity<String> sendTestEmail(@RequestParam Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    dailyEmailService.sendDailySummary(user);
                    return ResponseEntity.ok("Email sent to " + user.getEmail());
                })
                .orElse(ResponseEntity.badRequest().body("User not found"));
    }
}
