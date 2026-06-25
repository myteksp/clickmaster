package com.clicker.service;

import com.clicker.domain.*;
import com.clicker.dto.*;
import com.clicker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class SiteServiceTest {

    @Autowired
    private SiteService siteService;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        var user = new User.Builder()
            .email("sites-test@clicker.io")
            .passwordHash("hash")
            .name("Site Test User")
            .build();
        user = userRepository.save(user);
        userId = user.getId();
    }

    @Test
    void shouldCreateSite() {
        var dto = siteService.createSite(
            new SiteDto(null, null, "Example", "https://example.com"), userId);

        assertThat(dto.id()).isNotNull();
        assertThat(dto.name()).isEqualTo("Example");
        assertThat(dto.baseUrl()).isEqualTo("https://example.com");
        assertThat(dto.userId()).isEqualTo(userId);
    }

    @Test
    void shouldListUserSites() {
        siteService.createSite(new SiteDto(null, null, "Site A", "https://a.com"), userId);
        siteService.createSite(new SiteDto(null, null, "Site B", "https://b.com"), userId);

        var sites = siteService.getUserSites(userId);
        assertThat(sites).hasSize(2);
    }

    @Test
    void shouldUpdateSite() {
        var created = siteService.createSite(
            new SiteDto(null, null, "Old", "https://old.com"), userId);

        var updated = siteService.updateSite(created.id(),
            new SiteDto(null, null, "New", "https://new.com"), userId);

        assertThat(updated.name()).isEqualTo("New");
        assertThat(updated.baseUrl()).isEqualTo("https://new.com");
    }

    @Test
    void shouldDeleteSite() {
        var created = siteService.createSite(
            new SiteDto(null, null, "ToDelete", "https://del.com"), userId);

        siteService.deleteSite(created.id(), userId);

        assertThatThrownBy(() -> siteService.getSite(created.id(), userId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectAccessFromOtherUser() {
        var created = siteService.createSite(
            new SiteDto(null, null, "Mine", "https://mine.com"), userId);

        assertThatThrownBy(() -> siteService.getSite(created.id(), UUID.randomUUID()))
            .isInstanceOf(SecurityException.class);
    }
}
