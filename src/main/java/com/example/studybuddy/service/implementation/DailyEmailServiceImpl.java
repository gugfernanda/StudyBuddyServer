package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.EventRepository;
import com.example.studybuddy.repository.NotificationRepository;
import com.example.studybuddy.repository.TaskRepository;
import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.entity.Event;
import com.example.studybuddy.repository.entity.Task;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.DailyEmailService;
import com.example.studybuddy.service.NotificationService;
import jakarta.transaction.Transactional;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class DailyEmailServiceImpl implements DailyEmailService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final MessageSource messageSource;

    public DailyEmailServiceImpl(TaskRepository taskRepository, EventRepository eventRepository, JavaMailSender mailSender,
                                 UserRepository userRepository, NotificationService notificationService, NotificationRepository notificationRepository, MessageSource messageSource) {
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.messageSource = messageSource;
    }

    @Transactional
    @Override
    public void sendDailySummary(User user) {

        Locale locale = Locale.ENGLISH;
        if(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String langHeader = attrs.getRequest().getHeader("Accept-Language");
            if(langHeader != null) {
                locale = Locale.forLanguageTag(langHeader);
            }
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        notificationRepository.deleteByCreatedAtBefore(cutoff);

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Task> todayTasks = taskRepository.findByUserAndDeadline(user, today);
        List<Task> tomorrowTasks = taskRepository.findByUserAndDeadline(user, tomorrow);
        List<Event> todayEvents = eventRepository.findByUserAndStartTimeBetween(
                user,
                today.atStartOfDay(),
                today.atTime(23, 59)
        );

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Task task : todayTasks) {
            String message = messageSource.getMessage(
                    "notification.task.today",
                    new Object[]{task.getText()},
                    locale
            );
            notificationService.createNotification(user, message);
        }

        for (Task task : tomorrowTasks) {
            String message = messageSource.getMessage(
                    "notification.task.tomorrow",
                    new Object[]{task.getText()},
                    locale
            );
            notificationService.createNotification(user, message);
        }

        for (Event event : todayEvents) {
            String time = event.getStartTime().format(timeFormatter);
            String message = messageSource.getMessage(
                    "notification.event.today",
                    new Object[]{event.getTitle(), time},
                    locale
            );
            notificationService.createNotification(user, message);
        }

        StringBuilder content = new StringBuilder();

        content.append(messageSource.getMessage("email.greeting", new Object[]{user.getFullName()}, locale)).append("\n\n");

        content.append(messageSource.getMessage("email.events.title", null, locale)).append("\n");

        if(todayEvents.isEmpty()) {
            content.append(messageSource.getMessage("email.events.none", null, locale)).append("\n");
        } else {
            for(Event event : todayEvents) {
                content.append(messageSource.getMessage(
                        "email.event.line",
                        new Object[]{event.getTitle(), event.getStartTime().toLocalTime(),event.getEndTime().toLocalTime()},
                        locale
                )).append("\n");
                if(event.getDescription() != null && !event.getDescription().isEmpty()) {
                    content.append(messageSource.getMessage(
                            "email.event.description",
                            new Object[]{event.getDescription()},
                            locale
                    )).append("\n");
                }
            }
        }

        content.append("\n").append(messageSource.getMessage("email.tasks.today.title", null, locale)).append("\n")   ;
        if(todayTasks.isEmpty()) {
            content.append(messageSource.getMessage("email.tasks.today.none", null, locale)).append("\n");
        } else {
            for(Task task : todayTasks) {
                content.append(messageSource.getMessage("email.task.line", new Object[]{task.getText()}, locale)).append("\n");
            }
        }

        content.append("\n").append(messageSource.getMessage("email.tasks.tomorrow.title", null, locale)).append("\n")   ;

        if(tomorrowTasks.isEmpty()) {
            content.append(messageSource.getMessage("email.tasks.tomorrow.none", null, locale)).append("\n");
        } else {
            for(Task task : tomorrowTasks) {
                content.append(messageSource.getMessage("email.task.line", new Object[]{task.getText()}, locale)).append("\n");
            }
        }

        content.append("\n").append(messageSource.getMessage("email.footer", null, locale)).append("\n");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("StudyBuddy Daily Summary");
        message.setText(content.toString());

        mailSender.send(message);
    }

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void sendEmailsToAllUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        notificationRepository.deleteByCreatedAtBefore(cutoff);
        for(User user : userRepository.findAll()) {
            sendDailySummary(user);
        }
    }
}
