package com.example.studybuddy.repository;

import com.example.studybuddy.repository.entity.Task;
import com.example.studybuddy.repository.entity.TaskState;
import com.example.studybuddy.repository.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser(User user);
    List<Task> findByUserAndState(User user, TaskState state);

    List<Task> findByState(TaskState taskState);
    List<Task> findByUserAndDeadline(User user, LocalDate deadline);
    List<Task> findByDeadline(LocalDate deadline);
    List<Task> findByDeadlineBeforeAndStateNotAndOverdueNotifiedFalse(LocalDate date, TaskState doneState);

}
