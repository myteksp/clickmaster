package com.clicker.repository;

import com.clicker.domain.Campaign;
import com.clicker.domain.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    List<Campaign> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Campaign> findByStatus(CampaignStatus status);
    List<Campaign> findByUserIdAndStatus(UUID userId, CampaignStatus status);
}
