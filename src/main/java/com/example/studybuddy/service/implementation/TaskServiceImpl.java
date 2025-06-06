package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.TaskRepository;
import com.example.studybuddy.repository.entity.Task;
import com.example.studybuddy.repository.entity.TaskState;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.NotificationService;
import com.example.studybuddy.service.TaskService;
import com.google.gson.Gson;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final WebPushService webPushService;
    private final MessageSource messageSource;

    public TaskServiceImpl(TaskRepository taskRepository, NotificationService notificationService, WebPushService webPushService, MessageSource messageSource) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
        this.webPushService = webPushService;
        this.messageSource = messageSource;
    }

    @Override
    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    @Override
    public List<Task> getTasksByUser(User user) {
        List<Task> tasks = taskRepository.findByUser(user);
        return tasks;
    }

    @Override
    public List<Task> getTasksByUserAndState(User user, TaskState state) {
        return taskRepository.findByUserAndState(user, state);
    }

    @Override
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    @Override
    public Task updateTask(Task task) {
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @Override
    public ResponseEntity<?> updateTaskState(Long taskId, String newState) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if(!List.of("TO_DO", "IN_PROGRESS", "DONE").contains(newState)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task state");
        }

        task.setState(TaskState.valueOf(newState));

        if(newState.equals("DONE")) {
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setCompletedAt(null);
        }

        taskRepository.save(task);

        if (newState.equals("DONE")) {
            User user = task.getUser();

            Locale locale = Locale.ENGLISH;
            String langTag = user.getLanguage();
            if (langTag != null && !langTag.isBlank()) {
                locale = Locale.forLanguageTag(langTag);
            }

            String dbMessage = messageSource.getMessage("notification.task.completed", new Object[]{ task.getText() }, locale);
            notificationService.createNotification(task.getUser(), dbMessage);

            String title = messageSource.getMessage("push.task.done.title", null, locale);
            String body = messageSource.getMessage("push.task.done.body", new Object[]{ task.getText() }, locale);
            String url = messageSource.getMessage("push.task.done.url", null, locale);

            Map<String, String> data = Map.of("title", title, "body",  body, "url",   url);
            String payloadJson = new com.google.gson.Gson().toJson(data);

            try {
                Long userId = task.getUser().getId();
//                System.out.println("[TaskServiceImpl] Trimitem push pentru DONE la userId=" + userId +
//                        " (locale=" + locale + ")");
                webPushService.sendNotificationTo(userId, payloadJson);
            } catch (Exception e) {
                System.err.println("[TaskServiceImpl] Eroare trimitere push DONE: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of("message", "Task updated successfully"));
    }

    @Override
    public int deleteCompletedTasks() {
        List<Task> completedTasks = taskRepository.findByState(TaskState.DONE);
        int count = completedTasks.size();
        taskRepository.deleteAll(completedTasks);
        return count;
    }


}
