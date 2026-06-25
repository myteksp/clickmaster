package com.clicker.controller;

import com.clicker.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtService jwtService;

    protected String authToken;

    @BeforeEach
    void baseSetUp() {
        authToken = "Bearer " + jwtService.generateToken(
            java.util.UUID.randomUUID().toString(), "controller-test@clicker.io");
    }
}
