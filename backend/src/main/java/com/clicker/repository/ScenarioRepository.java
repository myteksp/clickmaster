package com.clicker.repository;

import com.clicker.domain.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ScenarioRepository extends JpaRepository<Scenario, UUID> {
    List<Scenario> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
