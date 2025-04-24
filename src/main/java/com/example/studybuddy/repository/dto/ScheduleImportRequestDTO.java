package com.example.studybuddy.repository.dto;

import java.time.LocalDate;

public class ScheduleImportRequestDTO {
    private String sheetUrl;
    private String sheetName;
    private String group;
    private LocalDate semesterEndDate;
    private String scheduleLabel;

    public String getSheetUrl() {
        return sheetUrl;
    }

    public void setSheetUrl(String sheetUrl) {
        this.sheetUrl = sheetUrl;
    }

    public LocalDate getSemesterEndDate() {
        return semesterEndDate;
    }

    public void setSemesterEndDate(LocalDate semesterEndDate) {
        this.semesterEndDate = semesterEndDate;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public String getScheduleLabel() {
        return scheduleLabel;
    }

    public void setScheduleLabel(String scheduleLabel) {
        this.scheduleLabel = scheduleLabel;
    }
}
