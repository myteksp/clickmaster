package com.clicker.service;

import com.clicker.domain.*;
import com.clicker.dto.ScenarioCreateRequest;
import com.clicker.dto.ScenarioDto;
import com.clicker.dto.StepDto;
import com.clicker.repository.ScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;

    public ScenarioService(ScenarioRepository scenarioRepository) {
        this.scenarioRepository = scenarioRepository;
    }

    @Transactional(readOnly = true)
    public List<ScenarioDto> getUserScenarios(UUID userId) {
        return scenarioRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScenarioDto getScenario(UUID id, UUID userId) {
        var scenario = scenarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found"));
        if (!scenario.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        return toDto(scenario);
    }

    @Transactional
    public ScenarioDto createScenario(ScenarioCreateRequest request, UUID userId) {
        var scenario = Scenario.builder()
            .userId(userId)
            .name(request.name())
            .description(request.description())
            .build();

        scenario = scenarioRepository.save(scenario);

        if (request.steps() != null) {
            for (int i = 0; i < request.steps().size(); i++) {
                var stepDto = request.steps().get(i);
                var step = ScenarioStep.builder()
                    .scenarioId(scenario.getId())
                    .scenario(scenario)
                    .orderIndex(i)
                    .actionType(stepDto.actionType())
                    .selector(stepDto.selector())
                    .value(stepDto.value())
                    .delayBeforeMs(stepDto.delayBeforeMs())
                    .delayAfterMs(stepDto.delayAfterMs())
                    .probability(stepDto.probability())
                    .config(stepDto.config())
                    .build();
                scenario.getSteps().add(step);
            }
        }

        scenario = scenarioRepository.save(scenario);
        return toDto(scenario);
    }

    @Transactional
    public ScenarioDto updateScenario(UUID id, ScenarioCreateRequest request, UUID userId) {
        var scenario = scenarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found"));
        if (!scenario.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }

        scenario.setName(request.name());
        scenario.setDescription(request.description());
        scenario.getSteps().clear();
        scenario = scenarioRepository.save(scenario);

        if (request.steps() != null) {
            for (int i = 0; i < request.steps().size(); i++) {
                var stepDto = request.steps().get(i);
                var step = ScenarioStep.builder()
                    .scenarioId(scenario.getId())
                    .scenario(scenario)
                    .orderIndex(i)
                    .actionType(stepDto.actionType())
                    .selector(stepDto.selector())
                    .value(stepDto.value())
                    .delayBeforeMs(stepDto.delayBeforeMs())
                    .delayAfterMs(stepDto.delayAfterMs())
                    .probability(stepDto.probability())
                    .config(stepDto.config())
                    .build();
                scenario.getSteps().add(step);
            }
        }

        scenario = scenarioRepository.save(scenario);
        return toDto(scenario);
    }

    @Transactional
    public void deleteScenario(UUID id, UUID userId) {
        var scenario = scenarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found"));
        if (!scenario.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        scenarioRepository.delete(scenario);
    }

    private ScenarioDto toDto(Scenario s) {
        List<StepDto> steps = s.getSteps().stream()
            .map(st -> new StepDto(
                st.getId(), st.getOrderIndex(), st.getActionType(),
                st.getSelector(), st.getValue(),
                st.getDelayBeforeMs(), st.getDelayAfterMs(),
                st.getProbability(), st.getConfig()
            ))
            .collect(Collectors.toList());

        return new ScenarioDto(s.getId(), s.getUserId(), s.getName(), s.getDescription(), steps);
    }
}
