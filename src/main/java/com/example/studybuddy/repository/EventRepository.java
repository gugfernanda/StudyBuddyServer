package com.example.studybuddy.repository;

import com.example.studybuddy.repository.entity.Event;
import com.example.studybuddy.repository.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByUserId(Long userId);
    List<Event> findByUser(User user);
}
