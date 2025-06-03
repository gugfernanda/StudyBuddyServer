package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.PushSubscriptionRepository;
import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.dto.PushSubscriptionDTO;
import com.example.studybuddy.repository.entity.PushSubscription;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.PushSubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PushSubscriptionServiceImpl implements PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;
    private final HttpServletRequest request;

    public PushSubscriptionServiceImpl(PushSubscriptionRepository pushSubscriptionRepository, UserRepository userRepository, HttpServletRequest request) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.userRepository = userRepository;
        this.request = request;
    }

    @Override
    public PushSubscription saveSubscription(PushSubscriptionDTO pushSubscriptionDTO) {
        HttpSession session = request.getSession(false);
        if(session == null || session.getAttribute("user") == null) {
            throw new RuntimeException("No user is logged in");
        }
        String username = session.getAttribute("user").toString();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long userId = user.getId();

        if (pushSubscriptionRepository.existsByUserIdAndEndpoint(userId, pushSubscriptionDTO.getEndpoint())) {
            return null;
        }


        PushSubscription pushSubscription = new PushSubscription();
        pushSubscription.setUserId(userId);
        pushSubscription.setEndpoint(pushSubscriptionDTO.getEndpoint());
        pushSubscription.setP256dh(pushSubscriptionDTO.getKeys().getP256dh());
        pushSubscription.setAuth(pushSubscriptionDTO.getKeys().getAuth());
        pushSubscription.setCreatedAt(LocalDateTime.now());
        return pushSubscriptionRepository.save(pushSubscription);

    }

    @Override
    @Transactional
    public void deleteByEndpoint(String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
    }
}
