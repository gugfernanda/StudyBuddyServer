package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.TaskRepository;
import com.example.studybuddy.repository.entity.Task;
import com.example.studybuddy.repository.entity.TaskState;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.implementation.WebPushService;

import jakarta.transaction.Transactional;

import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Service
public class DeadlineReminderService {

    private final TaskRepository taskRepository;
    private final WebPushService webPushService;
    private final MessageSource messageSource;

    public DeadlineReminderService(TaskRepository taskRepository,
                                   WebPushService webPushService,
                                   MessageSource messageSource) {
        this.taskRepository = taskRepository;
        this.webPushService = webPushService;
        this.messageSource = messageSource;
    }


    @Transactional
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDeadlineReminder() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Task> tasksExpiringTomorrow = taskRepository.findByDeadline(tomorrow);

        System.out.println("[DeadlineReminderService] Am găsit " +
                tasksExpiringTomorrow.size() + " sarcini cu deadline-ul: " + tomorrow);

        for (Task task : tasksExpiringTomorrow) {
            if (task.getState() != TaskState.DONE) {
                User user = task.getUser();
                Long userId = user.getId();

                String langTag = user.getLanguage();
                Locale locale = Locale.ENGLISH;

                if (langTag != null && !langTag.isBlank()) {
                    locale = Locale.forLanguageTag(langTag);
                }

                System.out.println("[DeadlineReminder] user=" + user.getUsername() +
                        " getLanguage()=" + langTag + " → locale=" + locale);

                String title = messageSource.getMessage(
                        "push.task.deadline.tomorrow.title",
                        null,
                        locale
                );
                String body = messageSource.getMessage(
                        "push.task.deadline.tomorrow.body",
                        new Object[]{ task.getText(), tomorrow.toString() },
                        locale
                );
                String url = messageSource.getMessage(
                        "push.task.deadline.tomorrow.url",
                        null,
                        locale
                );

                Map<String, String> data = Map.of(
                        "title", title,
                        "body",  body,
                        "url",   url
                );
                String payloadJson = new com.google.gson.Gson().toJson(data);

                try {
                    System.out.println("[DeadlineReminderService] Trimitem reminder push " +
                            "către userId=" + userId +
                            " (locale=" + locale + ") pentru taskId=" + task.getId());
                    webPushService.sendNotificationTo(userId, payloadJson);
                } catch (Exception e) {
                    System.err.println("[DeadlineReminderService] Eroare la trimiterea push-ului: "
                            + e.getMessage());
                }
            }
        }
    }
}
