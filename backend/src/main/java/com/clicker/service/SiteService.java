package com.clicker.service;

import com.clicker.domain.Site;
import com.clicker.dto.SiteDto;
import com.clicker.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SiteService {

    private final SiteRepository siteRepository;

    public SiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Transactional(readOnly = true)
    public List<SiteDto> getUserSites(UUID userId) {
        return siteRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SiteDto getSite(UUID id, UUID userId) {
        var site = siteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Site not found"));
        if (!site.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        return toDto(site);
    }

    @Transactional
    public SiteDto createSite(SiteDto dto, UUID userId) {
        var site = Site.builder()
            .userId(userId)
            .name(dto.name())
            .baseUrl(dto.baseUrl())
            .build();
        site = siteRepository.save(site);
        return toDto(site);
    }

    @Transactional
    public SiteDto updateSite(UUID id, SiteDto dto, UUID userId) {
        var site = siteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Site not found"));
        if (!site.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        site.setName(dto.name());
        site.setBaseUrl(dto.baseUrl());
        site = siteRepository.save(site);
        return toDto(site);
    }

    @Transactional
    public void deleteSite(UUID id, UUID userId) {
        var site = siteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Site not found"));
        if (!site.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        siteRepository.delete(site);
    }

    private SiteDto toDto(Site s) {
        return new SiteDto(s.getId(), s.getUserId(), s.getName(), s.getBaseUrl());
    }
}
