package com.example.studybuddy.repository.dto;

import java.time.LocalDate;

public class UrlImportRequestDTO {

    private String url;
    private String sheetName;
    private String groupName;
    private String series;
    private LocalDate startDate;
    private LocalDate endDate;

    public UrlImportRequestDTO() {
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public UrlImportRequestDTO(String url, String sheetName, String groupName, String series) {
        this.url = url;
        this.sheetName = sheetName;
        this.groupName = groupName;
        this.series = series;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }
}
