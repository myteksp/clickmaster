package com.clicker.controller;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseControllerTest {

    @Test
    void shouldRegisterUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content("""
                    {"email":"reg@test.com","password":"password123","name":"Test"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("reg@test.com"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void shouldLoginAfterRegister() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content("""
                    {"email":"login@test.com","password":"password123","name":"Login"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("""
                    {"email":"login@test.com","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content("""
                    {"email":"wrong@test.com","password":"correct","name":"User"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("""
                    {"email":"wrong@test.com","password":"incorrect"}
                    """))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content("""
                    {"email":"not-an-email","password":"password123","name":"Test"}
                    """))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldRejectShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content("""
                    {"email":"short@test.com","password":"12345","name":"Test"}
                    """))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldRequireAuthForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/campaigns"))
            .andExpect(status().isForbidden());
    }
}
