package com.clicker.controller;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class CampaignControllerTest extends BaseControllerTest {

    private String createSite() throws Exception {
        var result = mockMvc.perform(post("/api/sites")
                .header("Authorization", authToken)
                .contentType("application/json")
                .content("""
                    {"name":"Target Site","baseUrl":"https://example.com"}
                    """))
            .andExpect(status().isOk())
            .andReturn();
        return com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void shouldCreateCampaign() throws Exception {
        String siteId = createSite();

        mockMvc.perform(post("/api/campaigns")
                .header("Authorization", authToken)
                .contentType("application/json")
                .content(String.format("""
                    {
                      "siteId": "%s",
                      "name": "My Campaign",
                      "simulationLevel": "HTTP_ONLY",
                      "trafficPattern": "CONSTANT",
                      "visitsPerHour": 100,
                      "durationMinutes": 60,
                      "scheduleCron": null,
                      "geoDistribution": [
                        {"countryCode": "US", "weight": 60},
                        {"countryCode": "GB", "weight": 40}
                      ],
                      "deviceProfile": [
                        {"device": "desktop", "os": "Windows", "browser": "Chrome", "weight": 70}
                      ],
                      "userAgentConfig": {"rotation": "RANDOM", "customPool": []},
                      "proxyConfig": {"provider": "ASOCKS"},
                      "scenarios": []
                    }
                    """, siteId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("My Campaign"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.simulationLevel").value("HTTP_ONLY"))
            .andExpect(jsonPath("$.visitsPerHour").value(100))
            .andExpect(jsonPath("$.geoDistribution", hasSize(2)))
            .andExpect(jsonPath("$.geoDistribution[0].countryCode").value("US"));
    }

    @Test
    void shouldListAndDeleteCampaign() throws Exception {
        String siteId = createSite();

        var result = mockMvc.perform(post("/api/campaigns")
                .header("Authorization", authToken)
                .contentType("application/json")
                .content(String.format("""
                    {
                      "siteId": "%s",
                      "name": "Delete Me",
                      "simulationLevel": "HTTP_ONLY",
                      "trafficPattern": "CONSTANT",
                      "visitsPerHour": 10,
                      "durationMinutes": 5,
                      "geoDistribution": [],
                      "deviceProfile": [],
                      "userAgentConfig": {"rotation": "RANDOM", "customPool": []},
                      "proxyConfig": {"provider": "ASOCKS"},
                      "scenarios": []
                    }
                    """, siteId)))
            .andExpect(status().isOk())
            .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/campaigns")
                .header("Authorization", authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/campaigns/" + id)
                .header("Authorization", authToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldGetCampaignDetail() throws Exception {
        String siteId = createSite();

        var result = mockMvc.perform(post("/api/campaigns")
                .header("Authorization", authToken)
                .contentType("application/json")
                .content(String.format("""
                    {
                      "siteId": "%s",
                      "name": "Detail Test",
                      "simulationLevel": "FULL_BROWSER",
                      "trafficPattern": "PULSE",
                      "visitsPerHour": 500,
                      "durationMinutes": 30,
                      "geoDistribution": [
                        {"countryCode": "US", "weight": 100}
                      ],
                      "deviceProfile": [
                        {"device": "desktop", "os": "Windows", "browser": "Chrome", "weight": 100}
                      ],
                      "userAgentConfig": {"rotation": "RANDOM", "customPool": []},
                      "proxyConfig": {"provider": "ASOCKS"},
                      "scenarios": []
                    }
                    """, siteId)))
            .andExpect(status().isOk())
            .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/campaigns/" + id)
                .header("Authorization", authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulationLevel").value("FULL_BROWSER"))
            .andExpect(jsonPath("$.trafficPattern").value("PULSE"))
            .andExpect(jsonPath("$.visitsPerHour").value(500));
    }

    @Test
    void shouldRejectMissingSiteId() throws Exception {
        mockMvc.perform(post("/api/campaigns")
                .header("Authorization", authToken)
                .contentType("application/json")
                .content("""
                    {"name":"Bad Campaign"}
                    """))
            .andExpect(status().is4xxClientError());
    }
}
