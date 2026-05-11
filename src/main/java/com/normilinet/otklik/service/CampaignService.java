package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.enums.AnonymityMode;
import com.normilinet.otklik.domain.enums.CampaignMode;
import com.normilinet.otklik.domain.enums.CampaignStatus;
import com.normilinet.otklik.domain.enums.WorkStatus;
import com.normilinet.otklik.domain.model.Campaign;
import com.normilinet.otklik.domain.model.EvaluationCriterion;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.domain.model.Work;
import com.normilinet.otklik.domain.repository.CampaignRepository;
import com.normilinet.otklik.domain.repository.EvaluationCriterionRepository;
import com.normilinet.otklik.domain.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final EvaluationCriterionRepository criterionRepository;
    private final WorkRepository workRepository;
    private final com.normilinet.otklik.domain.repository.CampaignAttachmentRepository campaignAttachmentRepository;
    private final FileStorageService storage;

    @Transactional(readOnly = true)
    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Campaign> getActiveCampaigns() {
        return campaignRepository.findAllByStatus(CampaignStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Campaign> getActiveCampaignsForStudent(User student) {
        List<Campaign> all = campaignRepository.findAllByStatus(CampaignStatus.ACTIVE);
        java.util.Set<java.util.UUID> myTagIds = student.getTags() == null
                ? java.util.Set.of()
                : student.getTags().stream().map(t -> t.getId()).collect(java.util.stream.Collectors.toSet());
        List<Campaign> out = new ArrayList<>();
        for (Campaign c : all) {
            java.util.Set<com.normilinet.otklik.domain.model.Tag> ctags = c.getTags();
            if (ctags == null || ctags.isEmpty()) {
                out.add(c);
                continue;
            }
            boolean anyMatch = ctags.stream().anyMatch(t -> myTagIds.contains(t.getId()));
            if (anyMatch) out.add(c);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<Campaign> getByOrganizer(UUID organizerId) {
        return campaignRepository.findAllByOrganizerIdOrderByCreatedAtDesc(organizerId);
    }

    @Transactional(readOnly = true)
    public Campaign getCampaignById(UUID id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Кампания не найдена: " + id));
    }

    @Transactional
    public Campaign createCycle(User organizer,
                                String title,
                                String description,
                                CampaignMode mode,
                                AnonymityMode anonymity,
                                Integer scaleMax,
                                Integer expectedDurationDays,
                                LocalDateTime deadline,
                                List<CriterionInput> criteria,
                                java.util.Set<com.normilinet.otklik.domain.model.Tag> tags) {
        Campaign campaign = new Campaign();
        campaign.setTitle(title);
        campaign.setDescription(description);
        campaign.setMode(mode);
        campaign.setAnonymityMode(anonymity != null ? anonymity : AnonymityMode.OPEN);
        campaign.setScaleMax(scaleMax != null ? scaleMax : 10);
        campaign.setExpectedDurationDays(expectedDurationDays);
        campaign.setDeadline(deadline);
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setOrganizer(organizer);
        if (tags != null) campaign.getTags().addAll(tags);
        campaign = campaignRepository.save(campaign);
        saveCriteria(campaign, criteria);
        return campaign;
    }

    @Transactional
    public void updateCriteria(UUID campaignId, List<CriterionInput> criteria) {
        Campaign campaign = getCampaignById(campaignId);
        criterionRepository.deleteAllByCampaignId(campaignId);
        saveCriteria(campaign, criteria);
    }

    private void saveCriteria(Campaign campaign, List<CriterionInput> criteria) {
        if (criteria == null) return;
        BigDecimal total = BigDecimal.ZERO;
        int position = 0;
        List<EvaluationCriterion> toSave = new ArrayList<>();
        for (CriterionInput c : criteria) {
            if (c.name == null || c.name.isBlank()) continue;
            EvaluationCriterion ec = new EvaluationCriterion();
            ec.setCampaign(campaign);
            ec.setName(c.name.trim());
            ec.setDescription(c.description);
            BigDecimal w = c.weight != null ? c.weight : BigDecimal.ZERO;
            ec.setWeight(w.setScale(2, RoundingMode.HALF_UP));
            ec.setPosition(position++);
            total = total.add(w);
            toSave.add(ec);
        }
        if (!toSave.isEmpty() && total.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException("Сумма весов критериев должна быть 100. Сейчас: " + total);
        }
        criterionRepository.saveAll(toSave);
    }

    @Transactional
    public void startCampaign(UUID id) {
        Campaign c = getCampaignById(id);
        c.setStatus(CampaignStatus.ACTIVE);
        campaignRepository.save(c);
        List<Work> works = workRepository.findAllByCampaignIdAndStatus(id, WorkStatus.UPLOADED);
        for (Work w : works) {
            w.setStatus(WorkStatus.IN_QUEUE);
        }
        workRepository.saveAll(works);
    }

    @Transactional
    public void completeCampaign(UUID id) {
        Campaign c = getCampaignById(id);
        c.setStatus(CampaignStatus.COMPLETED);
        campaignRepository.save(c);
    }

    @Transactional(readOnly = true)
    public List<EvaluationCriterion> getCriteriaForCampaign(UUID campaignId) {
        return criterionRepository.findAllByCampaignIdOrderByPositionAsc(campaignId);
    }

    public record CriterionInput(String name, String description, BigDecimal weight) {}

    @Transactional
    public com.normilinet.otklik.domain.model.CampaignAttachment addFileMaterial(UUID campaignId,
                                                                                  org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        Campaign c = getCampaignById(campaignId);
        FileStorageService.StoredFile stored = storage.store(file, "campaigns/" + campaignId + "/materials");
        com.normilinet.otklik.domain.model.CampaignAttachment att = new com.normilinet.otklik.domain.model.CampaignAttachment();
        att.setCampaign(c);
        att.setKind(FileStorageService.classify(stored.originalName(), stored.mimeType()));
        att.setOriginalFilename(stored.originalName());
        att.setStoredPath(stored.relativePath());
        att.setMimeType(stored.mimeType() != null ? stored.mimeType() : FileStorageService.guessMime(stored.originalName()));
        att.setSizeBytes(stored.size());
        return campaignAttachmentRepository.save(att);
    }

    @Transactional
    public com.normilinet.otklik.domain.model.CampaignAttachment addLinkMaterial(UUID campaignId, String url) {
        Campaign c = getCampaignById(campaignId);
        com.normilinet.otklik.domain.model.CampaignAttachment att = new com.normilinet.otklik.domain.model.CampaignAttachment();
        att.setCampaign(c);
        att.setKind(com.normilinet.otklik.domain.enums.FileKind.LINK);
        att.setExternalUrl(url);
        return campaignAttachmentRepository.save(att);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.normilinet.otklik.domain.model.CampaignAttachment> getMaterials(UUID campaignId) {
        return campaignAttachmentRepository.findAllByCampaignIdOrderByCreatedAtAsc(campaignId);
    }
}
