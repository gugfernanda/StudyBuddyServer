package com.example.studybuddy.repository.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    @Column(unique = true)
    private String username;

    @JsonProperty("full_name")
    @Column(name = "full_name")
    private String fullName;

    @JsonProperty("email")
    @Column(unique = true)
    private String email;

    @JsonProperty("password")
    @Column(name = "password")
    private String password;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
