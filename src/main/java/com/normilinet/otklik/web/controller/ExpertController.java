package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.domain.enums.AnonymityMode;
import com.normilinet.otklik.domain.enums.AssignmentStatus;
import com.normilinet.otklik.domain.model.EvaluationCriterion;
import com.normilinet.otklik.domain.model.Review;
import com.normilinet.otklik.domain.model.ReviewAttachment;
import com.normilinet.otklik.domain.model.Work;
import com.normilinet.otklik.domain.model.WorkAssignment;
import com.normilinet.otklik.domain.model.WorkAttachment;
import com.normilinet.otklik.domain.repository.WorkAssignmentRepository;
import com.normilinet.otklik.security.CustomUserDetails;
import com.normilinet.otklik.service.AssignmentService;
import com.normilinet.otklik.service.CampaignService;
import com.normilinet.otklik.service.ReviewService;
import com.normilinet.otklik.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/expert")
@RequiredArgsConstructor
public class ExpertController {

    private final AssignmentService assignmentService;
    private final ReviewService reviewService;
    private final WorkService workService;
    private final CampaignService campaignService;
    private final WorkAssignmentRepository assignmentRepository;

    @GetMapping
    public String dashboard() {
        return "redirect:/expert/queue";
    }

    @GetMapping("/queue")
    public String queue(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        List<Work> available = assignmentService.listAvailableForReviewer(user.getUsername());
        List<WorkAssignment> mine = assignmentService.listMyAll(user.getUsername());
        long inProgress = mine.stream().filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS).count();
        long completed = mine.stream().filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).count();

        model.addAttribute("availableWorks", available.stream().map(this::asMaskedWork).toList());
        model.addAttribute("activeAssignments", mine.stream().filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS).toList());
        model.addAttribute("recentDone", mine.stream().filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).limit(10).toList());
        model.addAttribute("totalWorksAvailable", available.size());
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("completedCount", completed);
        return "expert/queue";
    }

    @PostMapping("/queue/{workId}/take")
    public String take(@AuthenticationPrincipal CustomUserDetails user, @PathVariable UUID workId) {
        WorkAssignment a = assignmentService.take(workId, user.getUsername());
        return "redirect:/expert/review/" + a.getId();
    }

    @GetMapping("/review/{assignmentId}")
    public String review(@AuthenticationPrincipal CustomUserDetails user,
                         @PathVariable UUID assignmentId,
                         Model model) {
        WorkAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Назначение не найдено"));
        if (!assignment.getReviewer().getUsername().equals(user.getUsername())) {
            throw new SecurityException("Не ваше назначение");
        }
        Work work = assignment.getWork();
        Review review = reviewService.getOrCreateDraft(assignmentId, user.getUsername());

        List<WorkAttachment> attachments = workService.getAttachments(work.getId());
        WorkAttachment primary = attachments.stream().filter(a -> !a.isVoice()).findFirst().orElse(null);
        WorkAttachment voice = attachments.stream().filter(WorkAttachment::isVoice).findFirst().orElse(null);

        List<EvaluationCriterion> criteria = campaignService.getCriteriaForCampaign(work.getCampaign().getId());
        Map<UUID, BigDecimal> scores = reviewService.currentScores(review.getId());
        List<ReviewAttachment> reviewAttachments = reviewService.getAttachments(review.getId());
        ReviewAttachment reviewVoice = reviewAttachments.stream().filter(ReviewAttachment::isVoice).findFirst().orElse(null);

        model.addAttribute("assignment", assignment);
        model.addAttribute("work", asMaskedWork(work));
        model.addAttribute("review", review);
        model.addAttribute("attachments", attachments);
        model.addAttribute("primaryAttachment", primary);
        model.addAttribute("workVoice", voice);
        model.addAttribute("criteria", criteria);
        model.addAttribute("scoresByCriterion", scores);
        model.addAttribute("reviewAttachments", reviewAttachments);
        model.addAttribute("reviewVoice", reviewVoice);
        model.addAttribute("scaleMax", work.getCampaign().getScaleMax());
        return "expert/review";
    }

    @PostMapping("/review/{assignmentId}/draft")
    public String saveDraft(@AuthenticationPrincipal CustomUserDetails user,
                            @PathVariable UUID assignmentId,
                            @RequestParam(required = false) String feedback,
                            @RequestParam(value = "criterionId", required = false) List<String> criterionIds,
                            @RequestParam(value = "criterionScore", required = false) List<String> criterionScores) {
        Map<UUID, BigDecimal> scoreMap = buildScores(criterionIds, criterionScores);
        reviewService.saveDraft(assignmentId, user.getUsername(), feedback, scoreMap);
        return "redirect:/expert/review/" + assignmentId + "?saved";
    }

    @PostMapping("/review/{assignmentId}/submit")
    public String submitFinal(@AuthenticationPrincipal CustomUserDetails user,
                              @PathVariable UUID assignmentId,
                              @RequestParam(required = false) String feedback,
                              @RequestParam(value = "criterionId", required = false) List<String> criterionIds,
                              @RequestParam(value = "criterionScore", required = false) List<String> criterionScores) {
        Map<UUID, BigDecimal> scoreMap = buildScores(criterionIds, criterionScores);
        reviewService.submitFinal(assignmentId, user.getUsername(), feedback, scoreMap);
        return "redirect:/expert/queue?submitted";
    }

    @PostMapping("/review/{assignmentId}/link")
    public String attachLink(@AuthenticationPrincipal CustomUserDetails user,
                             @PathVariable UUID assignmentId,
                             @RequestParam String url) {
        Review review = reviewService.getOrCreateDraft(assignmentId, user.getUsername());
        reviewService.attachLink(review.getId(), url);
        return "redirect:/expert/review/" + assignmentId;
    }

    @PostMapping("/queue/{assignmentId}/abandon")
    public String abandon(@AuthenticationPrincipal CustomUserDetails user, @PathVariable UUID assignmentId) {
        assignmentService.abandon(assignmentId, user.getUsername());
        return "redirect:/expert/queue";
    }

    private Map<UUID, BigDecimal> buildScores(List<String> ids, List<String> scores) {
        Map<UUID, BigDecimal> map = new HashMap<>();
        if (ids == null || scores == null) return map;
        for (int i = 0; i < ids.size() && i < scores.size(); i++) {
            String idRaw = ids.get(i);
            String valueRaw = scores.get(i);
            if (idRaw == null || valueRaw == null || valueRaw.isBlank()) continue;
            try {
                map.put(UUID.fromString(idRaw), new BigDecimal(valueRaw.replace(",", ".")));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return map;
    }

    private MaskedWork asMaskedWork(Work work) {
        AnonymityMode anon = work.getCampaign().getAnonymityMode();
        boolean hideAuthor = anon == AnonymityMode.SINGLE_BLIND || anon == AnonymityMode.DOUBLE_BLIND;
        String authorLabel = hideAuthor
                ? "#ANON-" + Math.abs(work.getId().getMostSignificantBits() % 10000)
                : work.getStudent().getUsername();
        return new MaskedWork(work, authorLabel);
    }

    public record MaskedWork(Work work, String authorLabel) {
        public UUID getId() { return work.getId(); }
        public String getTitle() { return work.getTitle(); }
        public String getStatus() { return work.getStatus().name(); }
        public String getCampaignTitle() { return work.getCampaign().getTitle(); }
    }
}
