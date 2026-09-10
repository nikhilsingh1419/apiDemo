package com.example.apidemo.service;

import com.example.apidemo.config.AuthProperties;
import com.example.apidemo.dto.*;
import com.example.apidemo.entity.AuthProvider;
import com.example.apidemo.entity.PasswordResetToken;
import com.example.apidemo.entity.User;
import com.example.apidemo.exception.ApiException;
import com.example.apidemo.repository.PasswordResetTokenRepository;
import com.example.apidemo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public AuthService(
            GoogleTokenVerifierService googleTokenVerifierService,
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            AuthProperties authProperties) {
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    @Transactional
    public AuthResponse authenticateWithGoogle(String idToken) {
        GoogleTokenVerifierService.GoogleUserInfo googleUser = googleTokenVerifierService.verify(idToken);
        User user = userRepository.findByGoogleSub(googleUser.googleId())
                .map(existing -> updateGoogleProfile(existing, googleUser))
                .orElseGet(() -> userRepository.findByEmail(googleUser.email())
                        .map(existing -> linkGoogleAccount(existing, googleUser))
                        .orElseGet(() -> createGoogleUser(googleUser)));

        return buildAuthResponse(user, authProperties.getJwtExpirationMs());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateRegistration(request);

        if (userRepository.existsByEmail(normalizeEmail(request.getEmail()))) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        User user = new User();
        user.setEmail(normalizeEmail(request.getEmail()));
        user.setName(request.getFullName().trim());
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);

        return buildAuthResponse(user, authProperties.getJwtExpirationMs());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request == null
                || request.getEmail() == null
                || request.getEmail().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email and password are required.");
        }

        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .filter(this::hasPassword)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        long expirationMs = Boolean.TRUE.equals(request.getRememberMe())
                ? authProperties.getJwtRememberMeExpirationMs()
                : authProperties.getJwtShortExpirationMs();
        return buildAuthResponse(user, expirationMs);
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required.");
        }
        if (!isValidEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required.");
        }

        userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .filter(this::hasPassword)
                .ifPresent(this::createPasswordResetToken);

        return new MessageResponse("If an account exists for this email, a reset link has been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        validatePasswordReset(request);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedAtIsNull(request.getToken())
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST, "Reset token is invalid or has expired."));

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        return new MessageResponse("Password has been reset successfully.");
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (request.getName() != null && request.getName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Name cannot be blank.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        if (request.getName() != null) {
            user.setName(request.getName().trim());
        }
        if (request.getPictureUrl() != null) {
            user.setPictureUrl(request.getPictureUrl());
        }
        return UserProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public MessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        if (request == null
                || request.getCurrentPassword() == null
                || request.getCurrentPassword().isBlank()
                || request.getNewPassword() == null
                || request.getNewPassword().isBlank()
                || request.getConfirmPassword() == null
                || request.getConfirmPassword().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "All password fields are required.");
        }
        if (request.getNewPassword().length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Password change is not available for Google sign-in accounts.");
        }
        if (!hasPassword(user) || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return new MessageResponse("Password updated successfully.");
    }

    public MessageResponse logout() {
        return new MessageResponse("Logged out successfully.");
    }

    @Transactional
    public AuthResponse createDevUser(DevCreateUserRequest request) {
        if (authProperties.isRequireJwt()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Dev endpoint is disabled when REQUIRE_JWT=true.");
        }
        if (request == null
                || request.getEmail() == null
                || request.getEmail().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email and password are required.");
        }
        if (request.getPassword().length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
        }
        if (!isValidEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A valid email is required.");
        }
        if (userRepository.existsByEmail(normalizeEmail(request.getEmail()))) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        User user = new User();
        user.setEmail(normalizeEmail(request.getEmail()));
        user.setName(resolveDevUserName(request));
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);
        return buildAuthResponse(user, authProperties.getJwtExpirationMs());
    }

    private String resolveDevUserName(DevCreateUserRequest request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            return request.getName().trim();
        }
        return request.getEmail().substring(0, request.getEmail().indexOf('@'));
    }

    private User createGoogleUser(GoogleTokenVerifierService.GoogleUserInfo googleUser) {
        User user = new User();
        user.setGoogleSub(googleUser.googleId());
        user.setEmail(googleUser.email());
        user.setName(googleUser.name());
        user.setPictureUrl(googleUser.pictureUrl());
        user.setAuthProvider(AuthProvider.GOOGLE);
        return userRepository.save(user);
    }

    private User linkGoogleAccount(User user, GoogleTokenVerifierService.GoogleUserInfo googleUser) {
        user.setGoogleSub(googleUser.googleId());
        user.setName(googleUser.name());
        user.setPictureUrl(googleUser.pictureUrl());
        if (user.getAuthProvider() == null) {
            user.setAuthProvider(AuthProvider.GOOGLE);
        }
        return userRepository.save(user);
    }

    private User updateGoogleProfile(User user, GoogleTokenVerifierService.GoogleUserInfo googleUser) {
        user.setEmail(googleUser.email());
        user.setName(googleUser.name());
        user.setPictureUrl(googleUser.pictureUrl());
        return userRepository.save(user);
    }

    private void createPasswordResetToken(User user) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(Instant.now().plusMillis(authProperties.getPasswordResetExpirationMs()));
        passwordResetTokenRepository.save(resetToken);
        log.info("Password reset token generated for user {}", user.getEmail());
    }

    private AuthResponse buildAuthResponse(User user, long expirationMs) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(jwtService.generateToken(user, expirationMs));
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtService.toExpirationSeconds(expirationMs));
        response.setUser(UserResponse.from(user));
        return response;
    }

    private void validateRegistration(RegisterRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Full name is required.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required.");
        }
        if (!isValidEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
        }
        if (request.getConfirmPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match.");
        }
        if (!request.isTermsAccepted()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You must agree to the Terms & Privacy Policy.");
        }
    }

    private void validatePasswordReset(ResetPasswordRequest request) {
        if (request == null
                || request.getToken() == null
                || request.getToken().isBlank()
                || request.getPassword() == null
                || request.getConfirmPassword() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset token is invalid or has expired.");
        }
        if (request.getPassword().length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match.");
        }
    }

    private boolean hasPassword(User user) {
        return user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(normalizeEmail(email)).matches();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
