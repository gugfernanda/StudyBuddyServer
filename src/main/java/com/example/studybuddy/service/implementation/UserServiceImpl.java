package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.dto.UserRequestDTO;
import com.example.studybuddy.repository.dto.UserResponseDTO;
import com.example.studybuddy.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
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

    public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }



    @Override
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        User user = modelMapper.map(userRequestDTO, User.class);
        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserResponseDTO.class);
    }


    @Override
    public UserResponseDTO getUserById(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        return userOptional.map(user -> modelMapper.map(user, UserResponseDTO.class)).orElse(null);
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

}
