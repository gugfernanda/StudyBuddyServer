package com.example.studybuddy.service;

public interface EmailService {

    public void sendResetEmail(String to, String resetLink);
}
