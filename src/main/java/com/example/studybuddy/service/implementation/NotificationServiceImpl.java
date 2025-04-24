package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.NotificationRepository;
import com.example.studybuddy.repository.dto.NotificationDTO;
import com.example.studybuddy.repository.entity.Notification;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void createNotification(User user, String message) {
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setMessage(message);
        notificationRepository.save(notif);
    }

    @Override
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    public long countUnread(User user) {
        return notificationRepository.countByUserAndReadIsFalse(user);
    }

    @Override
    public void markAllAsRead(User user) {
        List<Notification> notifs = getUserNotifications(user);
        for (Notification notif : notifs) {
            notif.setRead(true);
        }
        notificationRepository.saveAll(notifs);
    }

    @Override
    public List<NotificationDTO> toDTOList(List<Notification> notifications) {
        return notifications.stream()
                .map(n -> new NotificationDTO(
                        n.getId(),
                        n.getMessage(),
                        n.isRead(),
                        n.getCreatedAt(),
                        n.getUser().getUsername()
                ))
                .toList();
    }

}
