package com.clicker.service;

import com.clicker.dto.RegisterRequest;
import com.clicker.dto.LoginRequest;
import com.clicker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldRegisterUser() {
        var response = authService.register(
            new RegisterRequest("test@clicker.io", "password123", "Test User"));

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("test@clicker.io");
        assertThat(response.name()).isEqualTo("Test User");
        assertThat(response.token()).isNotBlank();
        assertThat(userRepository.existsByEmail("test@clicker.io")).isTrue();
    }

    @Test
    void shouldLoginAfterRegister() {
        authService.register(new RegisterRequest("login@clicker.io", "secret123", "Login User"));

        var response = authService.login(new LoginRequest("login@clicker.io", "secret123"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.email()).isEqualTo("login@clicker.io");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        authService.register(new RegisterRequest("dup@clicker.io", "pass1", "First"));

        assertThatThrownBy(() ->
            authService.register(new RegisterRequest("dup@clicker.io", "pass2", "Second"))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("already registered");
    }

    @Test
    void shouldRejectWrongPassword() {
        authService.register(new RegisterRequest("wrong@clicker.io", "correct", "User"));

        assertThatThrownBy(() ->
            authService.login(new LoginRequest("wrong@clicker.io", "wrong"))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Invalid email or password");
    }

    @Test
    void shouldRejectUnknownEmail() {
        assertThatThrownBy(() ->
            authService.login(new LoginRequest("nobody@clicker.io", "pass"))
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
