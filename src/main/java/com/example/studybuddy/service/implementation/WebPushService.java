package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.PushSubscriptionRepository;
import com.example.studybuddy.repository.entity.PushSubscription;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class WebPushService {

    private final PushSubscriptionRepository subscriptionRepository;
    private CustomPushService pushService;
    private final MessageSource messageSource;

    @Value("${push.vapid.public-key}")
    private String vapidPublicKeyBase64;

    @Value("${push.vapid.private-key}")
    private String vapidPrivateKeyBase64;

    @Value("${push.vapid.subject}")
    private String vapidSubject;

    public WebPushService(PushSubscriptionRepository subscriptionRepository, MessageSource messageSource) {
        this.subscriptionRepository = subscriptionRepository;
        this.messageSource = messageSource;
    }

    public String getVapidPublicKeyBase64() {
        return vapidPublicKeyBase64;
    }

    @PostConstruct
    public void init() throws GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        CustomPushService cps = new CustomPushService();
        cps.setSubject(vapidSubject);
        cps.setPublicKey(vapidPublicKeyBase64);
        cps.setPrivateKey(vapidPrivateKeyBase64);
        this.pushService = cps;
    }


    public void sendNotificationTo(Long userId, String payloadJson) {
        List<PushSubscription> subs = subscriptionRepository.findAllByUserId(userId);

        for (PushSubscription sub : subs) {
            try {
                Notification notification = new Notification(sub.getEndpoint(), sub.getP256dh(),
                        sub.getAuth(), payloadJson.getBytes(StandardCharsets.UTF_8)
                );

                HttpRequest webRequest = pushService.prepareRequestPublic(notification);

                Map<String, String> headers = webRequest.getHeaders();
                if (headers != null && headers.containsKey("Crypto-Key")) {
                    String cryptoHeader = headers.get("Crypto-Key");
                    String onlyP256ecdsa = Arrays.stream(cryptoHeader.split(";"))
                            .filter(s -> s.startsWith("p256ecdsa="))
                            .findFirst()
                            .orElse(cryptoHeader);
                    headers.put("Crypto-Key", onlyP256ecdsa);
                }

                HttpPost httpPost = new HttpPost(webRequest.getUrl());
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
                byte[] encryptedBody = webRequest.getBody();
                httpPost.setEntity(new ByteArrayEntity(encryptedBody));

                HttpResponse resp = HttpClientBuilder.create().build().execute(httpPost);
                int status = resp.getStatusLine().getStatusCode();
                System.out.println(" !!!!!!!!!! ==> status HTTP = " + status);

                if (status >= 400) {
                    String body = resp.getEntity() != null
                            ? new String(resp.getEntity().getContent().readAllBytes())
                            : "<fără body>";
                }

            } catch (JoseException | GeneralSecurityException e) {
                System.err.println("       *** eroare de criptare: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("       *** eroare la trimiterea HTTP: " + e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("410")) {
                    subscriptionRepository.delete(sub);
                    System.out.println("       -> subscription invalidă, ștersă din DB.");
                }
            }
        }
    }
}
