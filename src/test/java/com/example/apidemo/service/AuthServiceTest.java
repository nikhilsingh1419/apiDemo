package com.example.apidemo.service;

import com.example.apidemo.config.AuthProperties;
import com.example.apidemo.dto.AuthResponse;
import com.example.apidemo.dto.DevCreateUserRequest;
import com.example.apidemo.dto.LoginRequest;
import com.example.apidemo.dto.RegisterRequest;
import com.example.apidemo.entity.AuthProvider;
import com.example.apidemo.entity.User;
import com.example.apidemo.exception.ApiException;
import com.example.apidemo.repository.PasswordResetTokenRepository;
import com.example.apidemo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setJwtSecret("test-jwt-secret-key-with-at-least-32-bytes");
        authProperties.setJwtExpirationMs(3_600_000L);
        authProperties.setJwtRememberMeExpirationMs(604_800_000L);
        authProperties.setJwtShortExpirationMs(86_400_000L);

        JwtService jwtService = new JwtService(authProperties);
        jwtService.init();
        passwordEncoder = new BCryptPasswordEncoder(10);

        authService = new AuthService(
                googleTokenVerifierService,
                userRepository,
                passwordResetTokenRepository,
                jwtService,
                passwordEncoder,
                authProperties);
    }

    @Test
    void registerCreatesEmailUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPassword("secret123");
        request.setConfirmPassword("secret123");
        request.setTermsAccepted(true);

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("jane@example.com", response.getUser().getEmail());
        assertEquals("Jane Doe", response.getUser().getName());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(AuthProvider.EMAIL, userCaptor.getValue().getAuthProvider());
        assertTrue(passwordEncoder.matches("secret123", userCaptor.getValue().getPasswordHash()));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPassword("secret123");
        request.setConfirmPassword("secret123");
        request.setTermsAccepted(true);

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = new User();
        user.setId(2L);
        user.setEmail("jane@example.com");
        user.setName("Jane Doe");
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setPasswordHash(passwordEncoder.encode("secret123"));

        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("secret123");
        request.setRememberMe(true);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(request);

        assertNotNull(response.getAccessToken());
        assertEquals(604800L, response.getExpiresIn());
    }

    @Test
    void loginRejectsInvalidCredentials() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("wrong");

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void devCreateUserCreatesAccountWhenJwtNotRequired() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setJwtSecret("test-jwt-secret-key-with-at-least-32-bytes");
        authProperties.setRequireJwt(false);

        JwtService jwtService = new JwtService(authProperties);
        jwtService.init();

        AuthService devAuthService = new AuthService(
                googleTokenVerifierService,
                userRepository,
                passwordResetTokenRepository,
                jwtService,
                passwordEncoder,
                authProperties);

        DevCreateUserRequest request = new DevCreateUserRequest();
        request.setEmail("dev@example.com");
        request.setPassword("secret123");

        when(userRepository.existsByEmail("dev@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(9L);
            return user;
        });

        AuthResponse response = devAuthService.createDevUser(request);

        assertEquals("dev@example.com", response.getUser().getEmail());
        assertNotNull(response.getAccessToken());
    }
}
