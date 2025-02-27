package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.TaskRepository;
import com.example.studybuddy.repository.entity.Task;
import com.example.studybuddy.repository.entity.TaskState;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.TaskService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
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


}
