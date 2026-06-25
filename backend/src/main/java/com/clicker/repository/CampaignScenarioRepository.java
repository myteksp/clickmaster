package com.clicker.repository;

import com.clicker.domain.CampaignScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CampaignScenarioRepository extends JpaRepository<CampaignScenario, UUID> {
    List<CampaignScenario> findByCampaignId(UUID campaignId);
    void deleteByCampaignIdAndScenarioId(UUID campaignId, UUID scenarioId);
}
