package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.domain.model.Review;
import com.normilinet.otklik.domain.model.ReviewAttachment;
import com.normilinet.otklik.domain.model.Work;
import com.normilinet.otklik.domain.model.WorkAttachment;
import com.normilinet.otklik.domain.repository.WorkAssignmentRepository;
import com.normilinet.otklik.security.CustomUserDetails;
import com.normilinet.otklik.service.ReviewService;
import com.normilinet.otklik.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final WorkService workService;
    private final ReviewService reviewService;
    private final WorkAssignmentRepository assignmentRepository;

    @PostMapping("/work/{workId}")
    public ResponseEntity<?> uploadWorkVoice(@AuthenticationPrincipal CustomUserDetails user,
                                             @PathVariable UUID workId,
                                             @RequestParam("audio") MultipartFile audio,
                                             @RequestParam(value = "durationMs", required = false) Long durationMs) throws IOException {
        Work work = workService.getById(workId);
        if (!work.getStudent().getUsername().equals(user.getUsername())) {
            return ResponseEntity.status(403).body(Map.of("error", "Не ваша работа"));
        }
        WorkAttachment att = workService.attachVoice(work, audio.getBytes(), durationMs);
        return ResponseEntity.ok(Map.of("id", att.getId(), "url", "/files/work/" + att.getId()));
    }

    @PostMapping("/review/{assignmentId}")
    public ResponseEntity<?> uploadReviewVoice(@AuthenticationPrincipal CustomUserDetails user,
                                               @PathVariable UUID assignmentId,
                                               @RequestParam("audio") MultipartFile audio,
                                               @RequestParam(value = "durationMs", required = false) Long durationMs) throws IOException {
        var assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Назначение не найдено"));
        if (!assignment.getReviewer().getUsername().equals(user.getUsername())) {
            return ResponseEntity.status(403).body(Map.of("error", "Не ваше назначение"));
        }
        Review review = reviewService.getOrCreateDraft(assignmentId, user.getUsername());
        ReviewAttachment att = reviewService.attachVoice(review.getId(), audio.getBytes(), durationMs);
        return ResponseEntity.ok(Map.of("id", att.getId(), "url", "/files/review/" + att.getId()));
    }
}
