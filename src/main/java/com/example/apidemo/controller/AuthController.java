package com.example.apidemo.controller;

import com.example.apidemo.dto.*;
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

    @PostMapping("/dev/create-user")
    public ResponseEntity<AuthResponse> devCreateUser(@RequestBody DevCreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createDevUser(request));
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
    public ResponseEntity<?> getCurrentUser() {
        return SecurityUtils.optionalCurrentUser()
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(authService.getCurrentUser(user.userId())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse(
                                "No authenticated user. Login first or pass Authorization: Bearer <token>.")));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        return SecurityUtils.optionalCurrentUser()
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(
                        authService.updateProfile(user.userId(), request)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse(
                                "No authenticated user. Login first or pass Authorization: Bearer <token>.")));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        return SecurityUtils.optionalCurrentUser()
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(
                        authService.changePassword(user.userId(), request)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse(
                                "No authenticated user. Login first or pass Authorization: Bearer <token>.")));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok(authService.logout());
    }
}
