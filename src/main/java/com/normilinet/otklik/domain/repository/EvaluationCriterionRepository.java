package com.normilinet.otklik.domain.repository;

import com.normilinet.otklik.domain.model.EvaluationCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, UUID> {
    List<EvaluationCriterion> findAllByCampaignId(UUID campaignId);
}
