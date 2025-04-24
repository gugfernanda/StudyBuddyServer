package com.example.studybuddy.service;

import com.example.studybuddy.repository.entity.User;
import org.springframework.stereotype.Service;


public interface DailyEmailService {
    public void sendDailySummary(User user);
    public void sendEmailsToAllUsers();

}
