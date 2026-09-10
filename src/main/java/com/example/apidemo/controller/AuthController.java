package com.example.apidemo.controller;

import com.example.apidemo.dto.*;
import com.example.apidemo.security.AuthenticatedUser;
import com.example.apidemo.security.SecurityUtils;
import com.example.apidemo.service.AuthService;
import com.example.apidemo.service.InvalidGoogleTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(authService.getCurrentUser(currentUser.userId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(authService.updateProfile(currentUser.userId(), request));
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(@RequestBody ChangePasswordRequest request) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(authService.changePassword(currentUser.userId(), request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok(authService.logout());
    }
}
