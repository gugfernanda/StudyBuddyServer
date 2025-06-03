package com.example.studybuddy.controller;

import com.example.studybuddy.repository.PushSubscriptionRepository;
import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.dto.PushSubscriptionDTO;
import com.example.studybuddy.repository.entity.PushSubscription;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.PushSubscriptionService;
import com.example.studybuddy.service.implementation.WebPushService;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/push")
public class PushController {

    private final PushSubscriptionService pushSubscriptionService;
    private final UserRepository userRepository;
    private final WebPushService webPushService;


    public PushController(PushSubscriptionService pushSubscriptionService, UserRepository userRepository, WebPushService webPushService) {
        this.pushSubscriptionService = pushSubscriptionService;
        this.userRepository = userRepository;
        this.webPushService = webPushService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody PushSubscriptionDTO dto, HttpServletRequest request) {

        System.out.println("DTO endpoint = " + dto.getEndpoint());
        System.out.println("DTO p256dh   = " + (dto.getKeys() == null ? "keys=null" : dto.getKeys().getP256dh()));
        pushSubscriptionService.saveSubscription(dto);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestBody String endpoint) {
        pushSubscriptionService.deleteByEndpoint(endpoint);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test")
    public ResponseEntity<String> sendTestPush(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Nu e niciun utilizator logat în sesiune.");
        }
        String username = session.getAttribute("user").toString();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long userId = user.getId();

        String payloadJson = "{"
                + "\"title\":\"Test Push!\","
                + "\"body\":\"Aceasta este o notificare de test.\","
                + "\"url\":\"/tasks\""
                + "}";

        webPushService.sendNotificationTo(userId, payloadJson);
        return ResponseEntity.ok("Push test trimis către userId=" + userId);
    }

    @GetMapping("/vapidKey")
    public ResponseEntity<String> getVapidKey() {
        return ResponseEntity.ok(webPushService.getVapidPublicKeyBase64());
    }

    @PostMapping("/send/{userId}")
    public ResponseEntity<Void> sendPush(@PathVariable Long userId, @RequestBody Map<String, String> body
    ) {
        String payloadJson = new Gson().toJson(Map.of(
                "title", body.getOrDefault("title", "Notificare"),
                "body",  body.getOrDefault("message", "")
        ));
        webPushService.sendNotificationTo(userId, payloadJson);
        return ResponseEntity.ok().build();
    }


}
