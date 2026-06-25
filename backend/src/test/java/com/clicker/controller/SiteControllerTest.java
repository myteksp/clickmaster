package com.clicker.controller;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class SiteControllerTest extends BaseControllerTest {

    @Test
    void shouldCreateAndListSites() throws Exception {
        mockMvc.perform(post("/api/sites")
                .header("Authorization", authToken)
                .contentType("application/json")
                .content("""
                    {"name":"My Site","baseUrl":"https://example.com"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("My Site"))
            .andExpect(jsonPath("$.baseUrl").value("https://example.com"));

        mockMvc.perform(get("/api/sites")
                .header("Authorization", authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldUpdateSite() throws Exception {
        var result = mockMvc.perform(post("/api/sites")
                .header("Authorization", authToken)
                .contentType("application/json")
                .content("""
                    {"name":"Old","baseUrl":"https://old.com"}
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/sites/" + id)
                .header("Authorization", authToken)
                .contentType("application/json")
                .content("""
                    {"name":"New","baseUrl":"https://new.com"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New"))
            .andExpect(jsonPath("$.baseUrl").value("https://new.com"));
    }

    @Test
    void shouldDeleteSite() throws Exception {
        var result = mockMvc.perform(post("/api/sites")
                .header("Authorization", authToken)
                .contentType("application/json")
                .content("""
                    {"name":"ToDelete","baseUrl":"https://del.com"}
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/sites/" + id)
                .header("Authorization", authToken))
            .andExpect(status().isNoContent());
    }
}
