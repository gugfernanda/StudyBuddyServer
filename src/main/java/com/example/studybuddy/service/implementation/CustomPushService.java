package com.example.studybuddy.service.implementation;

import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.HttpRequest;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.security.GeneralSecurityException;


public class CustomPushService extends PushService {

    public HttpRequest prepareRequestPublic(Notification notification)
            throws GeneralSecurityException, IOException, JoseException {
        return super.prepareRequest(notification, Encoding.AES128GCM);
    }
}
