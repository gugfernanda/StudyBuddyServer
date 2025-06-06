package com.example.studybuddy.repository;

import com.example.studybuddy.repository.entity.Task;
import com.example.studybuddy.repository.entity.TaskState;
import com.example.studybuddy.repository.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser(User user);
    List<Task> findByUserAndState(User user, TaskState state);

    List<Task> findByState(TaskState taskState);
    List<Task> findByUserAndDeadline(User user, LocalDate deadline);
    List<Task> findByDeadline(LocalDate deadline);
    List<Task> findByDeadlineBeforeAndStateNotAndOverdueNotifiedFalse(LocalDate date, TaskState doneState);
    long countByUserIdAndStateAndCompletedAtBetween(Long userId, TaskState state, LocalDateTime start, LocalDateTime end);

    @Query("""
    SELECT FUNCTION('DATE', t.completedAt) AS day, COUNT(t) FROM Task t
    WHERE t.user.id = :userId AND t.state = 'DONE' AND t.completedAt BETWEEN :start AND :end
    GROUP BY FUNCTION('DATE', t.completedAt)ORDER BY FUNCTION('DATE', t.completedAt)
""")
    List<Object[]> countDoneGroupedByDay(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByUserIdAndStateNotAndDeadlineBefore(Long userId, TaskState excludedState, LocalDate referenceDate);

    @Query("""
    SELECT t.deadline AS day, COUNT(t) FROM Task t WHERE t.user.id = :userId AND t.state <> 'DONE' AND t.deadline BETWEEN :startDate AND :endDate
    GROUP BY t.deadline ORDER BY t.deadline
""")
    List<Object[]> countOverdueGroupedByDeadline(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT YEAR(t.completedAt) AS yr, MONTH(t.completedAt) AS mo, COUNT(t) FROM Task t
        WHERE t.user.id = :userId AND t.state = 'DONE' AND t.completedAt BETWEEN :start AND :end
        GROUP BY YEAR(t.completedAt), MONTH(t.completedAt)
        ORDER BY YEAR(t.completedAt), MONTH(t.completedAt)
    """)
    List<Object[]> countDoneGroupedByMonth(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
