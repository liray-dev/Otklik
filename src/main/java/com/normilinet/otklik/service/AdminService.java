package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.enums.WorkStatus;
import com.normilinet.otklik.domain.model.Campaign;
import com.normilinet.otklik.domain.model.CampaignAttachment;
import com.normilinet.otklik.domain.model.EvaluationCriterion;
import com.normilinet.otklik.domain.model.Review;
import com.normilinet.otklik.domain.model.ReviewAttachment;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.domain.model.Work;
import com.normilinet.otklik.domain.model.WorkAssignment;
import com.normilinet.otklik.domain.model.WorkAttachment;
import com.normilinet.otklik.domain.repository.CampaignAttachmentRepository;
import com.normilinet.otklik.domain.repository.CampaignRepository;
import com.normilinet.otklik.domain.repository.EvaluationCriterionRepository;
import com.normilinet.otklik.domain.repository.ReviewAttachmentRepository;
import com.normilinet.otklik.domain.repository.ReviewRepository;
import com.normilinet.otklik.domain.repository.ReviewScoreRepository;
import com.normilinet.otklik.domain.repository.UserRepository;
import com.normilinet.otklik.domain.repository.WorkAssignmentRepository;
import com.normilinet.otklik.domain.repository.WorkAttachmentRepository;
import com.normilinet.otklik.domain.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final CampaignRepository campaignRepository;
    private final EvaluationCriterionRepository criterionRepository;
    private final WorkRepository workRepository;
    private final WorkAttachmentRepository workAttachmentRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewScoreRepository reviewScoreRepository;
    private final ReviewAttachmentRepository reviewAttachmentRepository;
    private final CampaignAttachmentRepository campaignAttachmentRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;

    @Transactional
    public void resetCampaign(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Кампания не найдена"));
        List<Work> works = workRepository.findAllByCampaignId(campaignId);
        for (Work w : works) {
            deleteWorkCascade(w);
        }
    }

    @Transactional
    public void resetAll() {
        for (Campaign c : campaignRepository.findAll()) {
            for (Work w : workRepository.findAllByCampaignId(c.getId())) {
                deleteWorkCascade(w);
            }
            for (CampaignAttachment ca : campaignAttachmentRepository.findAllByCampaignIdOrderByCreatedAtAsc(c.getId())) {
                if (ca.getStoredPath() != null) {
                    safeDelete(ca.getStoredPath());
                }
            }
            campaignAttachmentRepository.deleteAll(campaignAttachmentRepository.findAllByCampaignIdOrderByCreatedAtAsc(c.getId()));
            for (EvaluationCriterion crit : criterionRepository.findAllByCampaignIdOrderByPositionAsc(c.getId())) {
                criterionRepository.delete(crit);
            }
            campaignRepository.delete(c);
        }
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        for (Work w : workRepository.findAllByStudentIdOrderByCreatedAtDesc(userId)) {
            deleteWorkCascade(w);
        }
        if (u.getAvatarPath() != null) {
            safeDelete(u.getAvatarPath());
        }
        userRepository.delete(u);
    }

    private void deleteWorkCascade(Work w) {
        for (WorkAssignment a : workAssignmentRepository.findAllByWorkId(w.getId())) {
            Review r = reviewRepository.findByAssignmentId(a.getId()).orElse(null);
            if (r != null) {
                reviewScoreRepository.deleteAll(reviewScoreRepository.findAllByReviewId(r.getId()));
                for (ReviewAttachment ra : reviewAttachmentRepository.findAllByReviewIdOrderByCreatedAtAsc(r.getId())) {
                    if (ra.getStoredPath() != null) safeDelete(ra.getStoredPath());
                }
                reviewAttachmentRepository.deleteAll(reviewAttachmentRepository.findAllByReviewIdOrderByCreatedAtAsc(r.getId()));
                reviewRepository.delete(r);
            }
            workAssignmentRepository.delete(a);
        }
        for (WorkAttachment wa : workAttachmentRepository.findAllByWorkIdOrderByCreatedAtAsc(w.getId())) {
            if (wa.getStoredPath() != null) safeDelete(wa.getStoredPath());
        }
        workAttachmentRepository.deleteAll(workAttachmentRepository.findAllByWorkIdOrderByCreatedAtAsc(w.getId()));
        workRepository.delete(w);
    }

    private void safeDelete(String storedPath) {
        try { storage.delete(storedPath); } catch (Exception ignored) {}
    }
}
