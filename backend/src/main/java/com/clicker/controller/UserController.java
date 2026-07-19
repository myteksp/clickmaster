package com.clicker.controller;

import com.clicker.domain.User;
import com.clicker.dto.AuthResponse;
import com.clicker.repository.UserRepository;
import com.clicker.service.AuthService;
import com.clicker.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@AuthenticationPrincipal String userId) {
        List<Map<String, Object>> users = userRepository.findAll().stream()
            .map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "email", u.getEmail(),
                "name", u.getName(),
                "enabled", u.isEnabled(),
                "createdAt", (Instant) u.getCreatedAt()
            ))
            .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<AuthResponse> create(@Valid @RequestBody RegisterRequest request,
                                                @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(authService.register(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        @AuthenticationPrincipal String currentUserId) {
        if (id.equals(UUID.fromString(currentUserId))) {
            throw new IllegalStateException("Cannot delete your own account");
        }
        var user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }
}
