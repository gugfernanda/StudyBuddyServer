package com.example.studybuddy.service;

public interface EmailService {

    public void sendVerificationCode(String to, String verificationCode);
}
