package com.example.studybuddy.controller;

import com.example.studybuddy.repository.dto.TaskStatsDTO;
import com.example.studybuddy.service.implementation.TaskStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stats")
@CrossOrigin(origins = "http://localhost:5173")
public class TaskStatsController {

    private final TaskStatsService taskStatsService;

    public TaskStatsController(TaskStatsService taskStatsService) {
        this.taskStatsService = taskStatsService;
    }

    @GetMapping("/{userId}/weekly")
    public ResponseEntity<TaskStatsDTO> getWeeklyStats(@PathVariable Long userId) {
        TaskStatsDTO dto = taskStatsService.getWeeklyStats(userId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{userId}/monthly")
    public ResponseEntity<TaskStatsDTO> getMonthlyStats(@PathVariable Long userId) {
        TaskStatsDTO dto = taskStatsService.getMonthlyStats(userId);
        return ResponseEntity.ok(dto);
    }
}
