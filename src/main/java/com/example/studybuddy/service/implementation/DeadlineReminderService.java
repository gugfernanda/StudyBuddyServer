package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.TaskRepository;
import com.example.studybuddy.repository.entity.Task;
import com.example.studybuddy.repository.entity.TaskState;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DeadlineReminderService {

    private final TaskRepository taskRepository;
    private final WebPushService  webPushService;

    public DeadlineReminderService(TaskRepository taskRepository, WebPushService webPushService) {
        this.taskRepository = taskRepository;
        this.webPushService = webPushService;
    }

    @Transactional
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDeadlineReminder() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Task> tasksExpiringTomorrow = taskRepository.findByDeadline(tomorrow);

        System.out.println("[DeadlineReminderService] Am găsit " +
                tasksExpiringTomorrow.size() + " sarcini cu deadline-ul: " + tomorrow);

        for(Task task : tasksExpiringTomorrow) {
            if(task.getState() != TaskState.DONE) {
                Long userId = task.getUser().getId();
                String title = "Reminder: Sarcina expiră mâine";
                String body = "Sarcina \"" + task.getText() + "\" expiră pe " + tomorrow + ".";
                String url = "/tasks";

                String payloadJson = "{"
                        + "\"title\":\"" + title + "\","
                        + "\"body\":\"" + body + "\","
                        + "\"url\":\"" + url + "\""
                        + "}";

                try {
                    System.out.println("    -> Trimit notificare push către userId=" + userId +
                            " pentru taskId=" + task.getId());
                    webPushService.sendNotificationTo(userId, payloadJson);
                } catch (Exception e) {
                    System.err.println("    !!! Eroare la trimiterea push-ului: " + e.getMessage());
                }
            }
        }
    }
}
