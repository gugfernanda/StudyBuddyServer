package com.example.studybuddy.controller;

import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.dto.UserRequestDTO;
import com.example.studybuddy.repository.dto.UserResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public UserController(UserService userService, UserRepository userRepository, ModelMapper modelMapper) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }



    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO savedUser = userService.createUser(userRequestDTO);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }






    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable long id) {
        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        UserResponseDTO userResponseDTO = modelMapper.map(user, UserResponseDTO.class);

        return new ResponseEntity<>(userResponseDTO, HttpStatus.OK);
    }



    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable long id) {
        boolean deleted = userService.deleteUserById(id);
        return deleted ? new ResponseEntity<>(HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }




    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }


    @GetMapping("/test")
    public String testRoute() {
        return "API is working!";
    }
}
