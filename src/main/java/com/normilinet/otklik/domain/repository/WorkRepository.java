package com.normilinet.otklik.domain.repository;

import com.normilinet.otklik.domain.model.Work;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkRepository extends JpaRepository<Work, UUID> {
    List<Work> findAllByCampaignId(UUID campaignId);
    List<Work> findAllByStudentId(UUID studentId);
}
