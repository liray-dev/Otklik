package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.domain.enums.AssignmentStatus;
import com.normilinet.otklik.domain.model.Campaign;
import com.normilinet.otklik.domain.model.Review;
import com.normilinet.otklik.domain.model.Work;
import com.normilinet.otklik.domain.model.WorkAssignment;
import com.normilinet.otklik.domain.repository.ReviewRepository;
import com.normilinet.otklik.domain.repository.WorkAssignmentRepository;
import com.normilinet.otklik.security.CustomUserDetails;
import com.normilinet.otklik.service.CampaignService;
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
import java.util.ArrayList;
import java.util.Base64;
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

    @GetMapping("/cycles")
    public String cycles(Model model) {
        model.addAttribute("campaigns", campaignService.getActiveCampaigns());
        return "student/cycles";
    }

    @GetMapping("/works")
    public String works(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        List<Work> works = workService.getWorksByStudent(user.getUsername());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Work w : works) {
            List<WorkAssignment> assignments = assignmentRepository.findAllByWorkId(w.getId());
            int reviewsDone = (int) assignments.stream().filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).count();
            List<Review> reviews = new ArrayList<>();
            for (WorkAssignment a : assignments) {
                reviewRepository.findByAssignmentId(a.getId()).ifPresent(reviews::add);
            }
            rows.add(Map.of(
                    "work", w,
                    "attachments", workService.getAttachments(w.getId()),
                    "assignmentsTotal", assignments.size(),
                    "reviewsDone", reviewsDone,
                    "reviews", reviews
            ));
        }
        model.addAttribute("rows", rows);
        return "student/works";
    }

    @GetMapping("/cycles/{id}/submit")
    public String submitForm(@PathVariable UUID id, Model model) {
        Campaign campaign = campaignService.getCampaignById(id);
        model.addAttribute("campaign", campaign);
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
}
