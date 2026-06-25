package com.clicker.controller;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class ScenarioControllerTest extends BaseControllerTest {

    @Test
    void shouldCreateScenarioWithSteps() throws Exception {
        mockMvc.perform(post("/api/scenarios")
                .header("Authorization", authToken)
                .contentType("application/json")
                .content("""
                    {
                      "name": "Browse pricing",
                      "description": "User explores pricing page",
                      "steps": [
                        {
                          "orderIndex": 0,
                          "actionType": "LOAD",
                          "selector": null,
                          "value": null,
                          "delayBeforeMs": 1000,
                          "delayAfterMs": 2000,
                          "probability": 1.0,
                          "config": null
                        },
                        {
                          "orderIndex": 1,
                          "actionType": "CLICK",
                          "selector": ".pricing-button",
                          "value": null,
                          "delayBeforeMs": 500,
                          "delayAfterMs": 1000,
                          "probability": 0.8,
                          "config": null
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Browse pricing"))
            .andExpect(jsonPath("$.steps", hasSize(2)))
            .andExpect(jsonPath("$.steps[1].actionType").value("CLICK"))
            .andExpect(jsonPath("$.steps[1].selector").value(".pricing-button"));
    }

    @Test
    void shouldListAndDeleteScenario() throws Exception {
        var result = mockMvc.perform(post("/api/scenarios")
                .header("Authorization", authToken)
                .contentType("application/json")
                .content("""
                    {"name":"S1","description":null,"steps":[]}
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/scenarios")
                .header("Authorization", authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/scenarios/" + id)
                .header("Authorization", authToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/scenarios")
                .header("Authorization", authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldUpdateScenario() throws Exception {
        var result = mockMvc.perform(post("/api/scenarios")
                .header("Authorization", authToken)
                .contentType("application/json")
                .content("""
                    {"name":"Original","description":"desc","steps":[]}
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/scenarios/" + id)
                .header("Authorization", authToken)
                .contentType("application/json")
                .content("""
                    {"name":"Updated","description":"new desc","steps":[]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated"))
            .andExpect(jsonPath("$.description").value("new desc"));
    }
}
