package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.TaskRepository;
import com.example.studybuddy.repository.entity.Task;
import com.example.studybuddy.repository.entity.TaskState;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.NotificationService;
import com.example.studybuddy.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    public TaskServiceImpl(TaskRepository taskRepository, NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
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
        taskRepository.save(task);

        if (newState.equals("DONE")) {
            notificationService.createNotification(task.getUser(), "Great job! You completed \"" + task.getText() + "\".");
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
