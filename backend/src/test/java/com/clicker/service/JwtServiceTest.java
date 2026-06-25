package com.clicker.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldGenerateAndValidateToken() {
        String token = jwtService.generateToken("user-123", "test@clicker.io");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUserId(token)).isEqualTo("user-123");
        assertThat(jwtService.validateToken(token, "user-123")).isTrue();
    }

    @Test
    void shouldRejectTokenWithWrongUserId() {
        String token = jwtService.generateToken("user-123", "test@clicker.io");

        assertThat(jwtService.validateToken(token, "user-456")).isFalse();
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken("user-123", "test@clicker.io");
        String tampered = token.substring(0, token.length() - 3) + "xyz";

        assertThat(jwtService.validateToken(tampered, "user-123")).isFalse();
    }
}
