package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.domain.enums.AnonymityMode;
import com.normilinet.otklik.domain.enums.AssignmentStatus;
import com.normilinet.otklik.domain.enums.CampaignMode;
import com.normilinet.otklik.domain.enums.WorkStatus;
import com.normilinet.otklik.domain.model.Campaign;
import com.normilinet.otklik.domain.model.EvaluationCriterion;
import com.normilinet.otklik.domain.model.Review;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.domain.model.Work;
import com.normilinet.otklik.domain.model.WorkAssignment;
import com.normilinet.otklik.domain.repository.ReviewRepository;
import com.normilinet.otklik.domain.repository.UserRepository;
import com.normilinet.otklik.domain.repository.WorkAssignmentRepository;
import com.normilinet.otklik.domain.repository.WorkRepository;
import com.normilinet.otklik.security.CustomUserDetails;
import com.normilinet.otklik.service.CampaignService;
import com.normilinet.otklik.service.CampaignService.CriterionInput;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/organizer")
@RequiredArgsConstructor
public class OrganizerController {

    private final CampaignService campaignService;
    private final WorkService workService;
    private final UserRepository userRepository;
    private final WorkRepository workRepository;
    private final WorkAssignmentRepository assignmentRepository;
    private final ReviewRepository reviewRepository;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        User organizer = userRepository.findByUsername(user.getUsername()).orElseThrow();
        List<Campaign> mine = campaignService.getByOrganizer(organizer.getId());
        model.addAttribute("campaigns", mine);
        model.addAttribute("active", mine.stream().filter(c -> c.getStatus().name().equals("ACTIVE")).count());
        model.addAttribute("draft", mine.stream().filter(c -> c.getStatus().name().equals("DRAFT")).count());
        model.addAttribute("completed", mine.stream().filter(c -> c.getStatus().name().equals("COMPLETED")).count());
        return "organizer/dashboard";
    }

    @GetMapping("/cycles")
    public String cyclesList(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        User organizer = userRepository.findByUsername(user.getUsername()).orElseThrow();
        model.addAttribute("campaigns", campaignService.getByOrganizer(organizer.getId()));
        return "organizer/cycles_list";
    }

    @GetMapping("/cycles/new")
    public String cycleNewForm() {
        return "organizer/cycle_new";
    }

    @PostMapping("/cycles/new")
    public String createCycle(@AuthenticationPrincipal CustomUserDetails user,
                              @RequestParam String title,
                              @RequestParam(required = false) String description,
                              @RequestParam(defaultValue = "EXPERT") String mode,
                              @RequestParam(defaultValue = "OPEN") String anonymity,
                              @RequestParam(required = false, defaultValue = "10") Integer scaleMax,
                              @RequestParam(required = false) String deadline,
                              @RequestParam(value = "criterionName", required = false) List<String> criterionNames,
                              @RequestParam(value = "criterionWeight", required = false) List<String> criterionWeights,
                              @RequestParam(value = "criterionDescription", required = false) List<String> criterionDescriptions,
                              @RequestParam(value = "links", required = false) List<String> links,
                              @RequestParam(value = "files", required = false) List<MultipartFile> files) throws IOException {
        User organizer = userRepository.findByUsername(user.getUsername()).orElseThrow();

        LocalDateTime deadlineDt = null;
        if (deadline != null && !deadline.isBlank()) {
            String s = deadline.trim();
            if (s.length() == 10) {
                deadlineDt = java.time.LocalDate.parse(s).atTime(23, 59, 59);
            } else {
                deadlineDt = LocalDateTime.parse(s);
            }
        }
        List<CriterionInput> criteria = mergeCriteriaInputs(criterionNames, criterionWeights, criterionDescriptions);

        Campaign campaign = campaignService.createCycle(
                organizer, title, description,
                CampaignMode.valueOf(mode),
                AnonymityMode.valueOf(anonymity),
                scaleMax, null, deadlineDt, criteria);

        if (files != null) {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                campaignService.addFileMaterial(campaign.getId(), f);
            }
        }
        if (links != null) {
            for (String link : links) {
                if (link == null || link.isBlank()) continue;
                campaignService.addLinkMaterial(campaign.getId(), link.trim());
            }
        }

        return "redirect:/organizer/cycles/" + campaign.getId();
    }

    @GetMapping("/cycles/{id}")
    public String view(@PathVariable UUID id, Model model) {
        Campaign campaign = campaignService.getCampaignById(id);
        List<EvaluationCriterion> criteria = campaignService.getCriteriaForCampaign(id);
        List<Work> works = workRepository.findAllByCampaignId(id);
        model.addAttribute("campaign", campaign);
        model.addAttribute("criteria", criteria);
        model.addAttribute("works", works);
        model.addAttribute("materials", campaignService.getMaterials(id));
        return "organizer/cycle_view";
    }

    @PostMapping("/cycles/{id}/start")
    public String start(@PathVariable UUID id) {
        campaignService.startCampaign(id);
        return "redirect:/organizer/cycles/" + id;
    }

    @PostMapping("/cycles/{id}/complete")
    public String complete(@PathVariable UUID id) {
        campaignService.completeCampaign(id);
        return "redirect:/organizer/cycles/" + id;
    }

    @GetMapping("/cycles/{id}/results")
    public String results(@PathVariable UUID id, Model model) {
        Campaign campaign = campaignService.getCampaignById(id);
        List<Work> works = workRepository.findAllByCampaignId(id);
        List<WorkAssignment> allAssignments = new ArrayList<>();
        for (Work w : works) {
            allAssignments.addAll(assignmentRepository.findAllByWorkId(w.getId()));
        }
        List<Review> reviews = new ArrayList<>();
        for (WorkAssignment a : allAssignments) {
            reviewRepository.findByAssignmentId(a.getId()).ifPresent(reviews::add);
        }

        long total = allAssignments.size();
        long done = allAssignments.stream().filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).count();
        long pending = total - done;
        double percent = total == 0 ? 0 : (done * 100.0 / total);

        BigDecimal avg = reviews.stream()
                .map(Review::getTotalScore)
                .filter(s -> s != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, reviews.stream().filter(r -> r.getTotalScore() != null).count())),
                        2, java.math.RoundingMode.HALF_UP);

        long[] distribution = new long[campaign.getScaleMax() + 1];
        for (Review r : reviews) {
            if (r.getTotalScore() != null) {
                int bucket = Math.min(campaign.getScaleMax(), r.getTotalScore().intValue());
                distribution[bucket]++;
            }
        }

        long inQueue = works.stream().filter(w -> w.getStatus() == WorkStatus.IN_QUEUE).count();
        long underReview = works.stream().filter(w -> w.getStatus() == WorkStatus.UNDER_REVIEW).count();
        long reviewed = works.stream().filter(w -> w.getStatus() == WorkStatus.REVIEWED).count();

        reviews.sort(Comparator.comparing(Review::getCreatedAt).reversed());

        model.addAttribute("campaign", campaign);
        model.addAttribute("works", works);
        model.addAttribute("reviews", reviews);
        model.addAttribute("totalAssignments", total);
        model.addAttribute("doneAssignments", done);
        model.addAttribute("pendingAssignments", pending);
        model.addAttribute("percentDone", percent);
        model.addAttribute("avgScore", avg);
        model.addAttribute("distribution", distribution);
        model.addAttribute("inQueue", inQueue);
        model.addAttribute("underReview", underReview);
        model.addAttribute("reviewed", reviewed);
        return "organizer/results";
    }

    private List<CriterionInput> mergeCriteriaInputs(List<String> names, List<String> weights, List<String> descriptions) {
        List<CriterionInput> out = new ArrayList<>();
        if (names == null) return out;
        for (int i = 0; i < names.size(); i++) {
            String n = names.get(i);
            if (n == null || n.isBlank()) continue;
            BigDecimal w = BigDecimal.ZERO;
            if (weights != null && i < weights.size()) {
                String raw = weights.get(i);
                if (raw != null && !raw.isBlank()) {
                    w = new BigDecimal(raw.replace(",", "."));
                }
            }
            String d = (descriptions != null && i < descriptions.size()) ? descriptions.get(i) : null;
            out.add(new CriterionInput(n, d, w));
        }
        return out;
    }

    private String stripExtension(String filename) {
        if (filename == null) return "Файл";
        int i = filename.lastIndexOf('.');
        return i > 0 ? filename.substring(0, i) : filename;
    }
}
