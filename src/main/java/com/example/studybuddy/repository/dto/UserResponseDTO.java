package com.example.studybuddy.repository.dto;

import com.example.studybuddy.repository.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@ToString
public class UserResponseDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("email")
    private String email;

    

    public UserResponseDTO(Long id, String username, String fullName, String email) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }

    public UserResponseDTO() {
    }

    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }
}
