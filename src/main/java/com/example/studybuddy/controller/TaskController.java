package com.example.studybuddy.controller;


import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.dto.TaskRequestDTO;
import com.example.studybuddy.repository.dto.TaskResponseDTO;
import com.example.studybuddy.repository.entity.Task;
import com.example.studybuddy.repository.entity.TaskState;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.TaskService;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tasks")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public TaskController(TaskService taskService, UserRepository userRepository, ModelMapper modelMapper) {
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTask(@RequestBody TaskRequestDTO taskRequestDTO) {

        Optional<User> userOptional = userRepository.findById(taskRequestDTO.getUser_id());
        if(userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        Task task = modelMapper.map(taskRequestDTO, Task.class);
        task.setUser(userOptional.get());

        Task savedTask = taskService.createTask(task);
        TaskResponseDTO responseDTO = modelMapper.map(savedTask, TaskResponseDTO.class);

        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<TaskResponseDTO>> getUserTasks(@PathVariable String username) {

        Optional<User> userOptional = userRepository.findByUsername(username);
        if(userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        List<TaskResponseDTO> tasks = taskService.getTasksByUser(userOptional.get())
                .stream()
                .map(task -> modelMapper.map(task, TaskResponseDTO.class))
                .collect(Collectors.toList());

        return ResponseEntity.ok(tasks);

    }

    @GetMapping("/user/{userId}/state/{state}")
    public ResponseEntity<List<TaskResponseDTO>> getUserTasksState(@PathVariable Long userId, @PathVariable TaskState state) {
        Optional<User> userOptional = userRepository.findById(userId);
        if(userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        List<TaskResponseDTO> tasks = taskService.getTasksByUserAndState(userOptional.get(), state)
                .stream()
                .map(task -> modelMapper.map(task, TaskResponseDTO.class))
                .collect(Collectors.toList());

        return ResponseEntity.ok(tasks);

    }

    @PutMapping("/update/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable Long taskId, @RequestBody TaskRequestDTO taskRequestDTO) {
        Optional<Task> existingTask = taskService.getTaskById(taskId);
        if(existingTask.isEmpty()) {
            return ResponseEntity.badRequest().body("Task not found");
        }

        Task task = existingTask.get();
        task.setText(taskRequestDTO.getText());
        task.setState(taskRequestDTO.getState());

        Task updatedTask = taskService.updateTask(task);
        TaskResponseDTO responseDTO = modelMapper.map(updatedTask, TaskResponseDTO.class);

        return ResponseEntity.ok(responseDTO);

    }

    @DeleteMapping("/delete/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId) {
        Optional<Task> existingTask = taskService.getTaskById(taskId);
        if(existingTask.isEmpty()) {
            return ResponseEntity.badRequest().body("Task not found");
        }

        taskService.deleteTask(taskId);
        return ResponseEntity.ok("Task deleted successfully");
    }

}
