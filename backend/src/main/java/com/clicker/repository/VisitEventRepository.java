package com.clicker.repository;

import com.clicker.domain.VisitEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VisitEventRepository extends JpaRepository<VisitEvent, UUID> {
    List<VisitEvent> findByCampaignRunIdOrderByCreatedAtDesc(UUID campaignRunId);
    long countByCampaignRunId(UUID campaignRunId);
    long countByCampaignRunIdAndSuccess(UUID campaignRunId, boolean success);
}
