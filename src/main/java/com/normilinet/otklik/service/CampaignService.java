package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.enums.CampaignMode;
import com.normilinet.otklik.domain.enums.CampaignStatus;
import com.normilinet.otklik.domain.model.Campaign;
import com.normilinet.otklik.domain.model.EvaluationCriterion;
import com.normilinet.otklik.domain.repository.CampaignRepository;
import com.normilinet.otklik.domain.repository.EvaluationCriterionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final EvaluationCriterionRepository criterionRepository;

    @Transactional(readOnly = true)
    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Campaign> getActiveCampaigns() {
        return campaignRepository.findAll(); // Could add a status filter here
    }

    @Transactional(readOnly = true)
    public Campaign getCampaignById(UUID id) {
        return campaignRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid campaign Id:" + id));
    }

    @Transactional
    public Campaign createCampaign(String title, String description, CampaignMode mode, LocalDateTime deadline) {
        Campaign campaign = new Campaign();
        campaign.setTitle(title);
        campaign.setDescription(description);
        campaign.setMode(mode);
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setDeadline(deadline);
        return campaignRepository.save(campaign);
    }
    
    @Transactional
    public void startCampaign(UUID id) {
        Campaign c = getCampaignById(id);
        c.setStatus(CampaignStatus.ACTIVE);
        campaignRepository.save(c);
    }

    @Transactional
    public EvaluationCriterion addCriterion(UUID campaignId, String name, String description, int maxScore) {
        Campaign campaign = getCampaignById(campaignId);
        EvaluationCriterion criterion = new EvaluationCriterion();
        criterion.setCampaign(campaign);
        criterion.setName(name);
        criterion.setDescription(description);
        criterion.setMaxScore(maxScore);
        return criterionRepository.save(criterion);
    }

    @Transactional(readOnly = true)
    public List<EvaluationCriterion> getCriteriaForCampaign(UUID campaignId) {
        return criterionRepository.findAll().stream()
                .filter(c -> c.getCampaign().getId().equals(campaignId))
                .toList();
    }
}
