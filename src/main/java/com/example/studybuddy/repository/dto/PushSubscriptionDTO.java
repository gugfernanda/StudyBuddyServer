package com.example.studybuddy.repository.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PushSubscriptionDTO {

    private String endpoint;
    private Keys keys;

    public PushSubscriptionDTO() {
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Keys getKeys() {
        return keys;
    }

    public void setKeys(Keys keys) {
        this.keys = keys;
    }

    public static class Keys {
        @JsonProperty("p256dh")
        private String p256dh;

        @JsonProperty("auth")
        private String auth;

        public Keys() {
        }

        public String getP256dh() {
            return p256dh;
        }

        public void setP256dh(String p256dh) {
            this.p256dh = p256dh;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }
    }
}
