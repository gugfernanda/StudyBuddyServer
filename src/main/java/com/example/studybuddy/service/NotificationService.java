package com.example.studybuddy.service;

import com.example.studybuddy.repository.dto.NotificationDTO;
import com.example.studybuddy.repository.entity.Notification;
import com.example.studybuddy.repository.entity.User;

import java.util.List;

public interface NotificationService {

    public void createNotification(User user, String message);
    public List<Notification> getUserNotifications(User user);
    public long countUnread(User user);
    public void markAllAsRead(User user);
    public List<NotificationDTO> toDTOList(List<Notification> notifications);

}
