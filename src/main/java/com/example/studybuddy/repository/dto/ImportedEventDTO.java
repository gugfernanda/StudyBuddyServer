package com.example.studybuddy.repository.dto;

public class ImportedEventDTO {

    private String dayOfWeek;
    private String title;
    private String description;
    private String startTime;
    private String endTime;

    public ImportedEventDTO(String dayOfWeek, String title, String description, String startTime, String endTime) {
        this.dayOfWeek = dayOfWeek;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public ImportedEventDTO() {
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
