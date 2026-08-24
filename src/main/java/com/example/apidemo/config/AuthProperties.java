package com.example.apidemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private List<String> googleClientIds = new ArrayList<>();
    private String jwtSecret = "";
    private long jwtExpirationMs = 604_800_000L;

    public List<String> getGoogleClientIds() {
        return googleClientIds;
    }

    public void setGoogleClientIds(List<String> googleClientIds) {
        this.googleClientIds = googleClientIds;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    public void setJwtExpirationMs(long jwtExpirationMs) {
        this.jwtExpirationMs = jwtExpirationMs;
    }
}
