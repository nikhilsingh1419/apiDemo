package com.example.apidemo.service;

import com.example.apidemo.config.AuthProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoogleTokenVerifierService {

    public record GoogleUserInfo(String googleId, String email, String name, String pictureUrl) {
    }

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(AuthProperties authProperties) {
        List<String> clientIds = authProperties.getGoogleClientIds().stream()
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();

        if (clientIds.isEmpty()) {
            throw new IllegalStateException(
                    "Google client IDs are not configured. Set GOOGLE_CLIENT_IDS environment variable.");
        }

        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(clientIds)
                .build();
    }

    public GoogleUserInfo verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new InvalidGoogleTokenException("Google ID token is required.");
        }

        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new InvalidGoogleTokenException("Invalid or expired Google ID token.");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                throw new InvalidGoogleTokenException("Google account email is not available.");
            }

            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");
            return new GoogleUserInfo(googleId, email, name, pictureUrl);
        } catch (InvalidGoogleTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidGoogleTokenException("Failed to verify Google ID token.");
        }
    }
}
