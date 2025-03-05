package com.example.studybuddy.repository.dto;

import com.example.studybuddy.repository.entity.TaskState;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskResponseDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("text")
    private String text;

    @JsonProperty("state")
    private TaskState state;

    @JsonProperty("user_id")
    private Long user_id;

    @JsonProperty("category_id")
    private Long category_id;

    @JsonProperty("deadline")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public Long getUser_id() {
        return user_id;
    }

    public void setUser_id(Long user_id) {
        this.user_id = user_id;
    }

    public Long getCategory_id() {
        return category_id;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public void setCategory_id(Long category_id) {
        this.category_id = category_id;
    }

}
