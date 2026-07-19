package com.clicker.controller;

import com.clicker.domain.User;
import com.clicker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private void createUser(String email, String password, String name) {
        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .name(name)
            .build();
        userRepository.save(user);
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        createUser("login@test.com", "password123", "Login User");

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("""
                    {"email":"login@test.com","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.email").value("login@test.com"));
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        createUser("wrong@test.com", "correct", "User");

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("""
                    {"email":"wrong@test.com","password":"incorrect"}
                    """))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldRejectUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("""
                    {"email":"nobody@test.com","password":"password123"}
                    """))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldRequireAuthForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/campaigns"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAccessToRegisterWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content("""
                    {"email":"hacker@test.com","password":"password123","name":"Hacker"}
                    """))
            .andExpect(status().isUnauthorized());
    }
}
