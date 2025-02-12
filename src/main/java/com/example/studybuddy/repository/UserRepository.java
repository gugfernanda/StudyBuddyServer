package com.example.studybuddy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.studybuddy.repository.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    void deleteById(Long id);
    List<User> findAll();
    Optional<User> findByEmail(String username);
    Optional<User> findByUsername(String emailOrUsername);
}
