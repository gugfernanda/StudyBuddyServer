package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.TaskRepository;
import com.example.studybuddy.repository.dto.TaskStatsDTO;
import com.example.studybuddy.repository.entity.TaskState;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.sql.Date;
import java.util.stream.Collectors;

@Service
public class TaskStatsService {
    private final TaskRepository taskRepository;

    public TaskStatsService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskStatsDTO getWeeklyStats(Long userId) {
        LocalDateTime endDateTime = LocalDateTime.now();
        LocalDateTime startDateTime = endDateTime.minusDays(6).with(LocalTime.MIN);

        long totalCompletedInWeek = taskRepository.countByUserIdAndStateAndCompletedAtBetween(userId, TaskState.DONE, startDateTime, endDateTime);

        List<TaskStatsDTO.CompletedPerDay> completedPerDay = taskRepository
                .countDoneGroupedByDay(userId, startDateTime, endDateTime)
                .stream()
                .map(row -> {
                    Date sqlDate = (Date) row[0];
                    Long count = (Long) row[1];
                    LocalDate ld = sqlDate.toLocalDate();
                    return new TaskStatsDTO.CompletedPerDay(ld, count);
                })
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        long overdueTodayCount = taskRepository.countByUserIdAndStateNotAndDeadlineBefore(userId, TaskState.DONE, today);

        return new TaskStatsDTO(overdueTodayCount, totalCompletedInWeek, completedPerDay);
    }

    public TaskStatsDTO getMonthlyStats(Long userId) {
        LocalDateTime endDateTime = LocalDateTime.now();
        LocalDateTime startDateTime = endDateTime.minusDays(29).with(LocalTime.MIN);

        long totalCompletedInMonth = taskRepository
                .countByUserIdAndStateAndCompletedAtBetween(userId, TaskState.DONE, startDateTime, endDateTime);

        List<TaskStatsDTO.CompletedPerDay> completedPerMonth = taskRepository
                .countDoneGroupedByMonth(userId, startDateTime, endDateTime)
                .stream()
                .map(row -> {
                    Integer year = ((Number) row[0]).intValue();
                    Integer month = ((Number) row[1]).intValue();
                    Long count = ((Number) row[2]).longValue();
                    LocalDate firstOfMonth = YearMonth.of(year, month).atDay(1);
                    return new TaskStatsDTO.CompletedPerDay(firstOfMonth, count);
                })
                .collect(Collectors.toList());

        long overdueTodayCount = taskRepository.countByUserIdAndStateNotAndDeadlineBefore(userId, TaskState.DONE, LocalDate.now());

        return new TaskStatsDTO(overdueTodayCount, totalCompletedInMonth, completedPerMonth);
    }
}
