package com.clicker.controller;

import com.clicker.dto.ScenarioCreateRequest;
import com.clicker.dto.ScenarioDto;
import com.clicker.service.ScenarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @GetMapping
    public ResponseEntity<List<ScenarioDto>> list(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(scenarioService.getUserScenarios(UUID.fromString(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScenarioDto> get(@PathVariable UUID id,
                                           @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(scenarioService.getScenario(id, UUID.fromString(userId)));
    }

    @PostMapping
    public ResponseEntity<ScenarioDto> create(@Valid @RequestBody ScenarioCreateRequest request,
                                              @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(scenarioService.createScenario(request, UUID.fromString(userId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScenarioDto> update(@PathVariable UUID id,
                                              @Valid @RequestBody ScenarioCreateRequest request,
                                              @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(scenarioService.updateScenario(id, request, UUID.fromString(userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal String userId) {
        scenarioService.deleteScenario(id, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }
}
