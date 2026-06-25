package com.clicker.service;

import com.clicker.domain.*;
import com.clicker.dto.*;
import com.clicker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ScenarioServiceTest {

    @Autowired
    private ScenarioService scenarioService;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        var user = new User.Builder()
            .email("scenario-test@clicker.io")
            .passwordHash("hash")
            .name("Scenario Test User")
            .build();
        user = userRepository.save(user);
        userId = user.getId();
    }

    @Test
    void shouldCreateScenarioWithSteps() {
        var steps = List.of(
            new StepDto(null, 0, StepAction.LOAD, null, null, 1000, 2000, 1.0, null),
            new StepDto(null, 1, StepAction.CLICK, ".button", null, 500, 1000, 0.8, null),
            new StepDto(null, 2, StepAction.SCROLL, null, null, 200, 500, 0.5, null)
        );
        var request = new ScenarioCreateRequest("Browse", "Test scenario", steps);

        var dto = scenarioService.createScenario(request, userId);

        assertThat(dto.id()).isNotNull();
        assertThat(dto.name()).isEqualTo("Browse");
        assertThat(dto.steps()).hasSize(3);
        assertThat(dto.steps().get(0).actionType()).isEqualTo(StepAction.LOAD);
        assertThat(dto.steps().get(1).actionType()).isEqualTo(StepAction.CLICK);
        assertThat(dto.steps().get(1).selector()).isEqualTo(".button");
        assertThat(dto.steps().get(1).probability()).isEqualTo(0.8);
    }

    @Test
    void shouldUpdateScenarioSteps() {
        var dto = scenarioService.createScenario(
            new ScenarioCreateRequest("Old", null, List.of(
                new StepDto(null, 0, StepAction.WAIT, null, null, 1000, 0, 1.0, null)
            )), userId);

        var updated = scenarioService.updateScenario(dto.id(),
            new ScenarioCreateRequest("Updated", "New desc", List.of(
                new StepDto(null, 0, StepAction.LOAD, null, null, 0, 0, 1.0, null),
                new StepDto(null, 1, StepAction.HOVER, ".menu", null, 500, 300, 0.7, null)
            )), userId);

        assertThat(updated.name()).isEqualTo("Updated");
        assertThat(updated.description()).isEqualTo("New desc");
        assertThat(updated.steps()).hasSize(2);
    }

    @Test
    void shouldDeleteScenario() {
        var dto = scenarioService.createScenario(
            new ScenarioCreateRequest("DeleteMe", null, List.of()), userId);

        scenarioService.deleteScenario(dto.id(), userId);

        assertThatThrownBy(() -> scenarioService.getScenario(dto.id(), userId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldListUserScenarios() {
        scenarioService.createScenario(
            new ScenarioCreateRequest("S1", null, List.of()), userId);
        scenarioService.createScenario(
            new ScenarioCreateRequest("S2", null, List.of()), userId);

        var list = scenarioService.getUserScenarios(userId);
        assertThat(list).hasSize(2);
    }

    @Test
    void shouldRejectOtherUserAccess() {
        var dto = scenarioService.createScenario(
            new ScenarioCreateRequest("Mine", null, List.of()), userId);

        assertThatThrownBy(() ->
            scenarioService.getScenario(dto.id(), UUID.randomUUID()))
            .isInstanceOf(SecurityException.class);
    }
}
