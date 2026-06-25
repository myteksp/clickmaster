package com.clicker.service;

import com.clicker.domain.User;
import com.clicker.dto.AuthResponse;
import com.clicker.dto.LoginRequest;
import com.clicker.dto.RegisterRequest;
import com.clicker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .name(request.name())
            .build();

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail());

        return new AuthResponse(user.getId(), user.getEmail(), user.getName(), token);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail());

        return new AuthResponse(user.getId(), user.getEmail(), user.getName(), token);
    }

    public User getCurrentUser(String userId) {
        return userRepository.findById(java.util.UUID.fromString(userId))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
