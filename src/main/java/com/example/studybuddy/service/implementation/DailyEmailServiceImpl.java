package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.EventRepository;
import com.example.studybuddy.repository.TaskRepository;
import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.entity.Event;
import com.example.studybuddy.repository.entity.Task;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.DailyEmailService;
import jakarta.transaction.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DailyEmailServiceImpl implements DailyEmailService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    public DailyEmailServiceImpl(TaskRepository taskRepository, EventRepository eventRepository, JavaMailSender mailSender, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    @Override
    public void sendDailySummary(User user) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Task> todayTasks = taskRepository.findByUserAndDeadline(user, today);
        List<Task> tomorrowTasks = taskRepository.findByUserAndDeadline(user, tomorrow);
        List<Event> todayEvents = eventRepository.findByUserAndStartTimeBetween(
                user,
                today.atStartOfDay(),
                today.atTime(23, 59)
        );

        StringBuilder content = new StringBuilder();
        content.append("Good morning, ").append(user.getFullName()).append("!\n\n");
        content.append("Events today:\n");

        if(todayEvents.isEmpty()) {
            content.append("No events today!\n");
        } else {
            for(Event event : todayEvents) {
                content.append("- ")
                        .append(event.getTitle())
                        .append(" (")
                        .append(event.getStartTime().toLocalTime())
                        .append(" - ")
                        .append(event.getEndTime().toLocalTime())
                        .append(")\n");
                if(event.getDescription() != null && !event.getDescription().isEmpty()) {
                    content.append(" > ").append(event.getDescription()).append("\n");
                }
            }
        }

        content.append("\nTasks due today:\n");
        if(todayTasks.isEmpty()) {
            content.append("No tasks today!\n");
        } else {
            for(Task task : todayTasks) {
                content.append("- ").append(task.getText()).append("\n");
            }
        }

        content.append("\nTasks due tomorrow:\n");
        if(tomorrowTasks.isEmpty()) {
            content.append("No tasks tomorrow!\n");
        } else {
            for(Task task : tomorrowTasks) {
                content.append("- ").append(task.getText()).append("\n");
            }
        }

        content.append("\nHave a productive day!\n");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("StudyBuddy Daily Summary");
        message.setText(content.toString());

        mailSender.send(message);
    }

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void sendEmailsToAllUsers() {
        for(User user : userRepository.findAll()) {
            sendDailySummary(user);
        }
    }
}
