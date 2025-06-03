package com.example.studybuddy.service;

import com.example.studybuddy.repository.dto.PushSubscriptionDTO;
import com.example.studybuddy.repository.entity.PushSubscription;

public interface PushSubscriptionService {

    PushSubscription saveSubscription(PushSubscriptionDTO pushSubscriptionDTO);

    void deleteByEndpoint(String endpoint);
}
