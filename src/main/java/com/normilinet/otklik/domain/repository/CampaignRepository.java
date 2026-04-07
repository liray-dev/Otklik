package com.normilinet.otklik.domain.repository;

import com.normilinet.otklik.domain.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
}
