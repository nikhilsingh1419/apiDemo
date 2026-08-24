package com.example.apidemo.controller;

import com.example.apidemo.dto.AuthResponse;
import com.example.apidemo.dto.GoogleLoginRequest;
import com.example.apidemo.service.AuthService;
import com.example.apidemo.service.InvalidGoogleTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody(required = false) GoogleLoginRequest request) {
        if (request == null || request.getIdToken() == null || request.getIdToken().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Request body with idToken is required.");
        }

        try {
            AuthResponse response = authService.authenticateWithGoogle(request.getIdToken());
            return ResponseEntity.ok(response);
        } catch (InvalidGoogleTokenException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        }
    }
}
