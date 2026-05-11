package com.normilinet.otklik.domain.repository;

import com.normilinet.otklik.domain.enums.CampaignStatus;
import com.normilinet.otklik.domain.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    List<Campaign> findAllByStatus(CampaignStatus status);
    List<Campaign> findAllByOrganizerIdOrderByCreatedAtDesc(UUID organizerId);
}
