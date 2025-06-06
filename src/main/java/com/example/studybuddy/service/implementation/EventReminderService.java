package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.EventRepository;
import com.example.studybuddy.repository.entity.Event;
import com.example.studybuddy.repository.entity.TaskState;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.NotificationService;

import jakarta.transaction.Transactional;

import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Service
public class EventReminderService {

    private final EventRepository eventRepository;
    private final WebPushService webPushService;
    private final NotificationService notificationService;
    private final MessageSource messageSource;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    public EventReminderService(EventRepository eventRepository,
                                WebPushService webPushService,
                                NotificationService notificationService,
                                MessageSource messageSource) {
        this.eventRepository = eventRepository;
        this.webPushService = webPushService;
        this.notificationService = notificationService;
        this.messageSource = messageSource;
    }


    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void sendEventReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);
        LocalDateTime from = oneHourLater.minusSeconds(30);
        LocalDateTime to   = oneHourLater.plusSeconds(30);

        List<Event> eventsInOneHour = eventRepository.findByStartTimeBetween(from, to);

        for (Event event : eventsInOneHour) {
            User user = event.getUser();
            Long userId = user.getId();

            Locale locale = Locale.ENGLISH;
            String langTag = user.getLanguage();
            if (langTag != null && !langTag.isBlank()) {
                locale = Locale.forLanguageTag(langTag);
            }

            String startTimeFormatted = event.getStartTime().format(timeFormatter);
            String dbMessage = messageSource.getMessage("notification.event.reminder", new Object[]{ event.getTitle(), startTimeFormatted }, locale);
            notificationService.createNotification(user, dbMessage);
            String pushTitle = messageSource.getMessage("push.event.reminder.title", null, locale);
            String pushBody = messageSource.getMessage("push.event.reminder.body", new Object[]{ event.getTitle(), startTimeFormatted }, locale);
            String pushUrl = messageSource.getMessage("push.event.reminder.url", null, locale);

            Map<String, String> payloadData = Map.of("title", pushTitle, "body",  pushBody, "url",   pushUrl);
            String payloadJson = new com.google.gson.Gson().toJson(payloadData);

            try {
                webPushService.sendNotificationTo(userId, payloadJson);
            } catch (Exception e) {
                System.err.println("[EventReminderService] Eroare la trimiterea push-ului: " + e.getMessage());
            }
        }
    }
}
