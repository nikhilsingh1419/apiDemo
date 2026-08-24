package com.example.apidemo.service;

import com.example.apidemo.dto.AuthResponse;
import com.example.apidemo.dto.UserResponse;
import com.example.apidemo.entity.User;
import com.example.apidemo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(
            GoogleTokenVerifierService googleTokenVerifierService,
            UserRepository userRepository,
            JwtService jwtService) {
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse authenticateWithGoogle(String idToken) {
        GoogleTokenVerifierService.GoogleUserInfo googleUser = googleTokenVerifierService.verify(idToken);
        User user = userRepository.findByGoogleId(googleUser.googleId())
                .map(existing -> updateProfile(existing, googleUser))
                .orElseGet(() -> createUser(googleUser));

        AuthResponse response = new AuthResponse();
        response.setAccessToken(jwtService.generateToken(user));
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtService.getExpirationSeconds());
        response.setUser(UserResponse.from(user));
        return response;
    }

    private User createUser(GoogleTokenVerifierService.GoogleUserInfo googleUser) {
        User user = new User();
        user.setGoogleId(googleUser.googleId());
        user.setEmail(googleUser.email());
        user.setName(googleUser.name());
        user.setPictureUrl(googleUser.pictureUrl());
        return userRepository.save(user);
    }

    private User updateProfile(User user, GoogleTokenVerifierService.GoogleUserInfo googleUser) {
        user.setEmail(googleUser.email());
        user.setName(googleUser.name());
        user.setPictureUrl(googleUser.pictureUrl());
        return userRepository.save(user);
    }
}
