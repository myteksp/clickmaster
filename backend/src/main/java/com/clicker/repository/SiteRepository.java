package com.clicker.repository;

import com.clicker.domain.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SiteRepository extends JpaRepository<Site, UUID> {
    List<Site> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
