package com.example.studybuddy.controller;


import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.dto.LoginRequestDTO;
import com.example.studybuddy.repository.dto.UserResponseDTO;
import com.example.studybuddy.repository.entity.User;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        String emailOrUsername = loginRequest.getEmailOrUsername();

        Optional<User> userOptional = userRepository.findByEmail(emailOrUsername);
        if(userOptional.isEmpty()) {
            userOptional = userRepository.findByUsername(emailOrUsername);
        }

        if(userOptional.isEmpty()) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        User user = userOptional.get();

        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return new ResponseEntity<>("Invalid password", HttpStatus.UNAUTHORIZED);
        }

        UserResponseDTO userResponseDTO = modelMapper.map(user, UserResponseDTO.class);
        return ResponseEntity.ok(userResponseDTO);
    }
}
