package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.domain.enums.AssignmentStatus;
import com.normilinet.otklik.domain.enums.ReviewStatus;
import com.normilinet.otklik.domain.model.Campaign;
import com.normilinet.otklik.domain.model.EvaluationCriterion;
import com.normilinet.otklik.domain.model.Review;
import com.normilinet.otklik.domain.model.Work;
import com.normilinet.otklik.domain.model.WorkAssignment;
import com.normilinet.otklik.domain.repository.ReviewRepository;
import com.normilinet.otklik.domain.repository.WorkAssignmentRepository;
import com.normilinet.otklik.security.CustomUserDetails;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final CampaignService campaignService;
    private final WorkService workService;
    private final WorkAssignmentRepository assignmentRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;

    @GetMapping("/cycles")
    public String cycles(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        java.util.List<Campaign> active = campaignService.getActiveCampaigns();
        java.util.List<Map<String, Object>> rows = new ArrayList<>();
        for (Campaign c : active) {
            java.util.Optional<Work> mine = workService.findExistingForStudent(c.getId(), user.getUsername());
            Map<String, Object> row = new HashMap<>();
            row.put("campaign", c);
            row.put("existing", mine.orElse(null));
            rows.add(row);
        }
        model.addAttribute("rows", rows);
        return "student/cycles";
    }

    @GetMapping("/works")
    public String works(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        List<Work> works = workService.getWorksByStudent(user.getUsername());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Work w : works) {
            List<WorkAssignment> assignments = assignmentRepository.findAllByWorkId(w.getId());
            int reviewsDone = (int) assignments.stream().filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).count();
            List<EvaluationCriterion> criteria = campaignService.getCriteriaForCampaign(w.getCampaign().getId());
            List<Map<String, Object>> reviewRows = new ArrayList<>();
            for (WorkAssignment a : assignments) {
                Review r = reviewRepository.findByAssignmentId(a.getId()).orElse(null);
                if (r == null || r.getStatus() != ReviewStatus.FINAL) continue;
                Map<UUID, BigDecimal> scores = reviewService.currentScores(r.getId());
                Map<String, Object> rr = new HashMap<>();
                rr.put("review", r);
                rr.put("assignment", a);
                rr.put("scores", scores);
                reviewRows.add(rr);
            }
            Map<String, Object> row = new HashMap<>();
            row.put("work", w);
            row.put("attachments", workService.getAttachments(w.getId()));
            row.put("assignmentsTotal", assignments.size());
            row.put("reviewsDone", reviewsDone);
            row.put("reviews", reviewRows);
            row.put("criteria", criteria);
            row.put("scaleMax", w.getCampaign().getScaleMax());
            rows.add(row);
        }
        model.addAttribute("rows", rows);
        return "student/works";
    }

    @GetMapping("/cycles/{id}/submit")
    public String submitForm(@PathVariable UUID id,
                             @AuthenticationPrincipal CustomUserDetails user,
                             @org.springframework.web.bind.annotation.RequestParam(value = "file", required = false) UUID activeFileId,
                             Model model) {
        Campaign campaign = campaignService.getCampaignById(id);
        java.util.Optional<Work> existing = workService.findExistingForStudent(id, user.getUsername());
        Work work = existing.orElse(null);
        boolean editable = work == null || workService.isEditable(work);

        java.util.List<com.normilinet.otklik.domain.model.WorkAttachment> all = work != null
                ? workService.getAttachments(work.getId())
                : java.util.List.of();
        java.util.List<com.normilinet.otklik.domain.model.WorkAttachment> documents = all.stream().filter(a -> !a.isVoice()).toList();
        com.normilinet.otklik.domain.model.WorkAttachment active = null;
        if (activeFileId != null) {
            active = documents.stream().filter(a -> a.getId().equals(activeFileId)).findFirst().orElse(null);
        }
        if (active == null) {
            active = documents.stream().findFirst().orElse(null);
        }

        String revisionComment = null;
        if (work != null && work.getStatus() == com.normilinet.otklik.domain.enums.WorkStatus.NEEDS_REVISION) {
            for (WorkAssignment a : assignmentRepository.findAllByWorkId(work.getId())) {
                if (a.getStatus() == AssignmentStatus.ABANDONED) {
                    Review r = reviewRepository.findByAssignmentId(a.getId()).orElse(null);
                    if (r != null && r.getFeedback() != null && !r.getFeedback().isBlank()) {
                        revisionComment = r.getFeedback();
                        break;
                    }
                }
            }
        }

        model.addAttribute("campaign", campaign);
        model.addAttribute("existing", work);
        model.addAttribute("attachments", documents);
        model.addAttribute("activeAttachment", active);
        model.addAttribute("editable", editable);
        model.addAttribute("materials", campaignService.getMaterials(id));
        model.addAttribute("revisionComment", revisionComment);
        return "student/submit";
    }

    @PostMapping("/cycles/{id}/submit")
    public String submit(@PathVariable UUID id,
                         @AuthenticationPrincipal CustomUserDetails user,
                         @RequestParam String title,
                         @RequestParam(required = false) String contentText,
                         @RequestParam(required = false) String externalLink,
                         @RequestParam(value = "files", required = false) List<MultipartFile> files,
                         @RequestParam(value = "voiceBase64", required = false) String voiceBase64) throws IOException {
        Work work = workService.submitWork(id, user.getUsername(), title, contentText, externalLink, files);
        if (voiceBase64 != null && !voiceBase64.isBlank()) {
            String b64 = voiceBase64.contains(",") ? voiceBase64.substring(voiceBase64.indexOf(',') + 1) : voiceBase64;
            try {
                byte[] audio = Base64.getDecoder().decode(b64);
                if (audio.length > 0) {
                    workService.attachVoice(work, audio, null);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return "redirect:/student/works?submitted";
    }

    @PostMapping("/works/{workId}/attachments/{attachmentId}/delete")
    public String deleteAttachment(@AuthenticationPrincipal CustomUserDetails user,
                                   @PathVariable UUID workId,
                                   @PathVariable UUID attachmentId) throws IOException {
        Work w = workService.getById(workId);
        workService.deleteAttachment(workId, attachmentId, user.getUsername());
        return "redirect:/student/cycles/" + w.getCampaign().getId() + "/submit";
    }
}
