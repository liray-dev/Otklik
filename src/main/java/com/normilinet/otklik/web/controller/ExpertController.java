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
    private final com.normilinet.otklik.service.NotificationService notificationService;

    @GetMapping
    public String dashboard() {
        return "redirect:/expert/queue";
    }

    @GetMapping("/queue")
    public String queue(@AuthenticationPrincipal CustomUserDetails user,
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) String campaign,
                        @RequestParam(required = false, defaultValue = "newest") String sort,
                        Model model) {
        List<Work> available = assignmentService.listAvailableForReviewer(user.getUsername());
        List<MaskedWork> filtered = available.stream()
                .map(this::asMaskedWork)
                .filter(mw -> q == null || q.isBlank() || mw.getTitle().toLowerCase().contains(q.toLowerCase()))
                .filter(mw -> campaign == null || campaign.isBlank() || mw.getCampaignTitle().toLowerCase().contains(campaign.toLowerCase()))
                .toList();
        java.util.Comparator<MaskedWork> cmp = java.util.Comparator.comparing(mw -> mw.work().getCreatedAt());
        if ("newest".equals(sort)) cmp = cmp.reversed();
        else if ("title".equals(sort)) cmp = java.util.Comparator.comparing(MaskedWork::getTitle, String.CASE_INSENSITIVE_ORDER);
        filtered = filtered.stream().sorted(cmp).toList();

        List<WorkAssignment> mine = assignmentService.listMyAll(user.getUsername());
        long inProgress = mine.stream().filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS).count();
        long completed = mine.stream().filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).count();

        model.addAttribute("availableWorks", filtered);
        model.addAttribute("totalWorksAvailable", available.size());
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("completedCount", completed);
        model.addAttribute("filterQ", q);
        model.addAttribute("filterCampaign", campaign);
        model.addAttribute("filterSort", sort);
        return "expert/queue";
    }

    @GetMapping("/in-progress")
    public String inProgress(@AuthenticationPrincipal CustomUserDetails user,
                             @RequestParam(required = false) String q,
                             Model model) {
        List<WorkAssignment> mine = assignmentService.listMyAll(user.getUsername());
        List<WorkAssignment> active = mine.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS)
                .filter(a -> q == null || q.isBlank()
                        || (a.getWork().getTitle() != null && a.getWork().getTitle().toLowerCase().contains(q.toLowerCase()))
                        || a.getWork().getCampaign().getTitle().toLowerCase().contains(q.toLowerCase()))
                .toList();
        long inProgress = mine.stream().filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS).count();
        long completed = mine.stream().filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).count();
        long available = assignmentService.listAvailableForReviewer(user.getUsername()).size();

        model.addAttribute("activeAssignments", active);
        model.addAttribute("totalWorksAvailable", available);
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("completedCount", completed);
        model.addAttribute("filterQ", q);
        return "expert/in_progress";
    }

    @GetMapping("/completed")
    public String completed(@AuthenticationPrincipal CustomUserDetails user,
                            @RequestParam(required = false) String q,
                            @RequestParam(required = false, defaultValue = "newest") String sort,
                            Model model) {
        List<WorkAssignment> mine = assignmentService.listMyAll(user.getUsername());
        List<WorkAssignment> done = mine.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED)
                .filter(a -> q == null || q.isBlank()
                        || (a.getWork().getTitle() != null && a.getWork().getTitle().toLowerCase().contains(q.toLowerCase()))
                        || a.getWork().getCampaign().getTitle().toLowerCase().contains(q.toLowerCase()))
                .sorted((x, y) -> {
                    if ("oldest".equals(sort)) {
                        return java.util.Objects.compare(x.getCompletedAt(), y.getCompletedAt(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
                    }
                    return java.util.Objects.compare(y.getCompletedAt(), x.getCompletedAt(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
                })
                .toList();
        long completed = done.size();
        long inProgress = mine.stream().filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS).count();
        long available = assignmentService.listAvailableForReviewer(user.getUsername()).size();

        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (WorkAssignment a : done) {
            Review r = reviewService.findByAssignment(a.getId()).orElse(null);
            List<EvaluationCriterion> criteria = campaignService.getCriteriaForCampaign(a.getWork().getCampaign().getId());
            Map<UUID, BigDecimal> scores = r != null ? reviewService.currentScores(r.getId()) : Map.of();
            rows.add(Map.of(
                    "assignment", a,
                    "review", r != null ? r : new Review(),
                    "hasReview", r != null,
                    "criteria", criteria,
                    "scores", scores,
                    "scaleMax", a.getWork().getCampaign().getScaleMax()
            ));
        }

        model.addAttribute("doneRows", rows);
        model.addAttribute("totalWorksAvailable", available);
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("completedCount", completed);
        model.addAttribute("filterQ", q);
        model.addAttribute("filterSort", sort);
        return "expert/completed";
    }

    @PostMapping("/review/{assignmentId}/reopen")
    public String reopen(@AuthenticationPrincipal CustomUserDetails user, @PathVariable UUID assignmentId) {
        WorkAssignment a = reviewService.reopen(assignmentId, user.getUsername());
        return "redirect:/expert/review/" + a.getId();
    }

    @PostMapping("/queue/{workId}/take")
    public String take(@AuthenticationPrincipal CustomUserDetails user, @PathVariable UUID workId) {
        WorkAssignment a = assignmentService.take(workId, user.getUsername());
        AnonymityMode anon = a.getWork().getCampaign().getAnonymityMode();
        boolean hideReviewer = anon == AnonymityMode.DOUBLE_BLIND;
        String who = hideReviewer ? "Рецензент" : user.getUsername();
        notificationService.push(a.getWork().getStudent(),
                "WORK_TAKEN",
                who + " взял вашу работу «" + a.getWork().getTitle() + "» на проверку.",
                "/student/works");
        return "redirect:/expert/review/" + a.getId();
    }

    @GetMapping("/review/{assignmentId}")
    public String review(@AuthenticationPrincipal CustomUserDetails user,
                         @PathVariable UUID assignmentId,
                         @RequestParam(value = "file", required = false) UUID activeFileId,
                         Model model) {
        WorkAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Назначение не найдено"));
        if (!assignment.getReviewer().getUsername().equals(user.getUsername())) {
            throw new SecurityException("Не ваше назначение");
        }
        Work work = assignment.getWork();
        Review review = reviewService.getOrCreateDraft(assignmentId, user.getUsername());

        List<WorkAttachment> all = workService.getAttachments(work.getId());
        List<WorkAttachment> documents = all.stream().filter(a -> !a.isVoice()).toList();
        WorkAttachment active = null;
        if (activeFileId != null) {
            active = documents.stream().filter(a -> a.getId().equals(activeFileId)).findFirst().orElse(null);
        }
        if (active == null) {
            active = documents.stream().findFirst().orElse(null);
        }
        WorkAttachment voice = all.stream().filter(WorkAttachment::isVoice).findFirst().orElse(null);

        List<EvaluationCriterion> criteria = campaignService.getCriteriaForCampaign(work.getCampaign().getId());
        Map<UUID, BigDecimal> scores = reviewService.currentScores(review.getId());
        List<ReviewAttachment> reviewAttachments = reviewService.getAttachments(review.getId());
        ReviewAttachment reviewVoice = reviewAttachments.stream().filter(ReviewAttachment::isVoice).findFirst().orElse(null);

        model.addAttribute("assignment", assignment);
        model.addAttribute("work", asMaskedWork(work));
        model.addAttribute("review", review);
        model.addAttribute("attachments", documents);
        model.addAttribute("activeAttachment", active);
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

    @PostMapping("/review/{assignmentId}/autosave")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> autosave(@AuthenticationPrincipal CustomUserDetails user,
                                        @PathVariable UUID assignmentId,
                                        @RequestParam(required = false) String feedback,
                                        @RequestParam(value = "criterionId", required = false) List<String> criterionIds,
                                        @RequestParam(value = "criterionScore", required = false) List<String> criterionScores) {
        Map<UUID, BigDecimal> scoreMap = buildScores(criterionIds, criterionScores);
        reviewService.saveDraft(assignmentId, user.getUsername(), feedback, scoreMap);
        return Map.of("ok", true, "savedAt", java.time.LocalDateTime.now().toString());
    }

    @PostMapping("/review/{assignmentId}/submit")
    public String submitFinal(@AuthenticationPrincipal CustomUserDetails user,
                              @PathVariable UUID assignmentId,
                              @RequestParam(required = false) String feedback,
                              @RequestParam(value = "criterionId", required = false) List<String> criterionIds,
                              @RequestParam(value = "criterionScore", required = false) List<String> criterionScores) {
        Map<UUID, BigDecimal> scoreMap = buildScores(criterionIds, criterionScores);
        Review r = reviewService.submitFinal(assignmentId, user.getUsername(), feedback, scoreMap);
        WorkAssignment a = r.getAssignment();
        notificationService.push(a.getWork().getStudent(),
                "REVIEW_FINAL",
                "Получена финальная рецензия на работу «" + a.getWork().getTitle() + "».",
                "/student/works");
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

    @PostMapping("/review/{assignmentId}/revision")
    public String sendForRevision(@AuthenticationPrincipal CustomUserDetails user,
                                  @PathVariable UUID assignmentId,
                                  @RequestParam(required = false) String comment) {
        Work w = reviewService.sendBackForRevision(assignmentId, user.getUsername(), comment);
        String msg = "Ваша работа «" + w.getTitle() + "» отправлена на доработку рецензентом " + user.getUsername() + ".";
        notificationService.push(w.getStudent(), "NEEDS_REVISION", msg, "/student/cycles/" + w.getCampaign().getId() + "/submit");
        return "redirect:/expert/queue?revision";
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
