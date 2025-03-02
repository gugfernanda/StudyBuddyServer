package com.example.studybuddy.service;

import com.example.studybuddy.repository.entity.Task;
import com.example.studybuddy.repository.entity.TaskState;
import com.example.studybuddy.repository.entity.User;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

public interface TaskService {
    Task createTask(Task task);
    List<Task> getTasksByUser(User user);
    List<Task> getTasksByUserAndState(User user, TaskState state);
    Task updateTask(Task task);
    void deleteTask(Long id);
    Optional<Task> getTaskById(Long id);
    public ResponseEntity<?> updateTaskState(Long taskId, String newState);
    public int deleteCompletedTasks();
}
