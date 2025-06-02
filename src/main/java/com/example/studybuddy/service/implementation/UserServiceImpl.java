package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.dto.UserRequestDTO;
import com.example.studybuddy.repository.dto.UserResponseDTO;
import com.example.studybuddy.repository.dto.UserUpdateDTO;
import com.example.studybuddy.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        User user = modelMapper.map(userRequestDTO, User.class);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserResponseDTO.class);
    }

    @Override
    public boolean deleteUserById(Long id) {
        if(userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }


    @Override
    public UserResponseDTO getUserById(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        return userOptional.map(user -> modelMapper.map(user, UserResponseDTO.class)).orElse(null);
    }



    @Override
    public List<UserResponseDTO> getAllUsers() {
        List<User> users = userRepository.findAll();

        List<UserResponseDTO> dtos = users.stream()
                .map(user -> {
                    UserResponseDTO dto = modelMapper.map(user, UserResponseDTO.class);

                    /*System.out.println("Mapped DTO - ID: " + dto.getId() +
                            ", Username: " + dto.getUsername() +
                            ", Full Name: " + dto.getFullName() +
                            ", Email: " + dto.getEmail());*/

                    return dto;
                })
                .collect(Collectors.toList());

        return dtos;
    }

    @Override
    public UserResponseDTO updateUser(Long id, UserUpdateDTO dto) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found with id " + id));

        if(dto.getNewPassword() != null && !dto.getNewPassword().isEmpty()) {
            if(!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
                throw new BadCredentialsException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }

        if(dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }

        if(dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if(dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }

        User updatedUser = userRepository.save(user);
        return new UserResponseDTO(updatedUser);
    }

}
