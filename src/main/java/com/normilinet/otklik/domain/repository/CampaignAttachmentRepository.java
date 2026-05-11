package com.normilinet.otklik.domain.repository;

import com.normilinet.otklik.domain.model.CampaignAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignAttachmentRepository extends JpaRepository<CampaignAttachment, UUID> {
    List<CampaignAttachment> findAllByCampaignIdOrderByCreatedAtAsc(UUID campaignId);
}
