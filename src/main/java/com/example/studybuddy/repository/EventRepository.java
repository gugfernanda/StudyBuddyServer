package com.example.studybuddy.repository;

import com.example.studybuddy.repository.entity.Event;
import com.example.studybuddy.repository.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByUserId(Long userId);
    List<Event> findByUser(User user);
    void deleteAllByUserAndScheduleLabelAndImportedIsTrue(User user, String scheduleLabel);

    @Modifying
    @Transactional
    void deleteAllByUserAndScheduleLabel(User user, String scheduleLabel);

    List<Event> findByUserAndStartTimeBetween(User user, LocalDateTime localDateTime, LocalDateTime localDateTime1);
}
