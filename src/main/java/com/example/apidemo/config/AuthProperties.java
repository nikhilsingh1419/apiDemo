package com.example.apidemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private List<String> googleClientIds = new ArrayList<>();
    private String jwtSecret = "";
    private long jwtExpirationMs = 604_800_000L;
    private long jwtRememberMeExpirationMs = 604_800_000L;
    private long jwtShortExpirationMs = 86_400_000L;
    private long passwordResetExpirationMs = 3_600_000L;
    private boolean requireJwt = false;

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

    public long getJwtRememberMeExpirationMs() {
        return jwtRememberMeExpirationMs;
    }

    public void setJwtRememberMeExpirationMs(long jwtRememberMeExpirationMs) {
        this.jwtRememberMeExpirationMs = jwtRememberMeExpirationMs;
    }

    public long getJwtShortExpirationMs() {
        return jwtShortExpirationMs;
    }

    public void setJwtShortExpirationMs(long jwtShortExpirationMs) {
        this.jwtShortExpirationMs = jwtShortExpirationMs;
    }

    public long getPasswordResetExpirationMs() {
        return passwordResetExpirationMs;
    }

    public void setPasswordResetExpirationMs(long passwordResetExpirationMs) {
        this.passwordResetExpirationMs = passwordResetExpirationMs;
    }

    public boolean isRequireJwt() {
        return requireJwt;
    }

    public void setRequireJwt(boolean requireJwt) {
        this.requireJwt = requireJwt;
    }
}
