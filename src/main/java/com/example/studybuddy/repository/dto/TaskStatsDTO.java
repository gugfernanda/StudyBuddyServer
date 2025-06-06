package com.example.studybuddy.repository.dto;

import java.time.LocalDate;
import java.util.List;

public class TaskStatsDTO {

    private List<CompletedPerDay> completedStats;
    private long totalCompleted;
    private long overdueToday;

    public TaskStatsDTO(long overdueToday, long totalCompleted, List<CompletedPerDay> completedStats) {
        this.overdueToday = overdueToday;
        this.totalCompleted = totalCompleted;
        this.completedStats = completedStats;
    }

    public List<CompletedPerDay> getCompletedStats() {
        return completedStats;
    }

    public void setCompletedStats(List<CompletedPerDay> completedStats) {
        this.completedStats = completedStats;
    }

    public long getTotalCompleted() {
        return totalCompleted;
    }

    public void setTotalCompleted(long totalCompleted) {
        this.totalCompleted = totalCompleted;
    }

    public long getOverdueToday() {
        return overdueToday;
    }

    public void setOverdueToday(long overdueToday) {
        this.overdueToday = overdueToday;
    }

    public static class CompletedPerDay {
     private LocalDate date;
     private long count;

        public CompletedPerDay(LocalDate date, long count) {
            this.date = date;
            this.count = count;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}
