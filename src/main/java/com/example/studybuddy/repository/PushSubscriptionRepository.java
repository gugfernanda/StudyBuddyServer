package com.example.studybuddy.repository;

import com.example.studybuddy.repository.dto.PushSubscriptionDTO;
import com.example.studybuddy.repository.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    List<PushSubscription> findAllByUserId(Long userId);
    void deleteByEndpoint(String endpoint);

    boolean existsByUserIdAndEndpoint(Long userId, String endpoint);
}
