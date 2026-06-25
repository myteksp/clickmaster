package com.clicker.repository;

import com.clicker.domain.CampaignRun;
import com.clicker.domain.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRunRepository extends JpaRepository<CampaignRun, UUID> {
    List<CampaignRun> findByCampaignIdOrderByStartedAtDesc(UUID campaignId);
    Optional<CampaignRun> findTopByCampaignIdAndStatusOrderByStartedAtDesc(UUID campaignId, RunStatus status);
    List<CampaignRun> findByStatus(RunStatus status);
}
