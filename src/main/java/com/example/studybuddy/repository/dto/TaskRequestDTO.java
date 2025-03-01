package com.example.studybuddy.repository.dto;

import com.example.studybuddy.repository.entity.TaskState;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskRequestDTO {
    @JsonProperty("text")
    private String text;

    @JsonProperty("state")
    private TaskState state;

    @JsonProperty("user_id")
    private Long user_id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("category_id")
    private Long category_id;

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

    public void setCategory_id(Long category_id) {
        this.category_id = category_id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}


