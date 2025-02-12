package com.example.studybuddy.service;


import com.example.studybuddy.repository.dto.UserRequestDTO;
import com.example.studybuddy.repository.dto.UserResponseDTO;
import com.example.studybuddy.repository.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {

    boolean deleteUserById(Long id);
    UserResponseDTO createUser(UserRequestDTO userRequestDTO);
    UserResponseDTO getUserById(Long id);
    List<UserResponseDTO> getAllUsers();

}
