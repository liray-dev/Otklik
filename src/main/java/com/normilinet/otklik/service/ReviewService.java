package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.enums.AssignmentStatus;
import com.normilinet.otklik.domain.enums.ReviewStatus;
import com.normilinet.otklik.domain.enums.WorkStatus;
import com.normilinet.otklik.domain.model.EvaluationCriterion;
import com.normilinet.otklik.domain.model.Review;
import com.normilinet.otklik.domain.model.ReviewAttachment;
import com.normilinet.otklik.domain.model.ReviewScore;
import com.normilinet.otklik.domain.model.Work;
import com.normilinet.otklik.domain.model.WorkAssignment;
import com.normilinet.otklik.domain.repository.EvaluationCriterionRepository;
import com.normilinet.otklik.domain.repository.ReviewAttachmentRepository;
import com.normilinet.otklik.domain.repository.ReviewRepository;
import com.normilinet.otklik.domain.repository.ReviewScoreRepository;
import com.normilinet.otklik.domain.repository.WorkAssignmentRepository;
import com.normilinet.otklik.domain.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewScoreRepository scoreRepository;
    private final ReviewAttachmentRepository attachmentRepository;
    private final WorkAssignmentRepository assignmentRepository;
    private final WorkRepository workRepository;
    private final EvaluationCriterionRepository criterionRepository;
    private final FileStorageService storage;

    @Transactional
    public Review getOrCreateDraft(UUID assignmentId, String reviewerUsername) {
        WorkAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Назначение не найдено"));
        if (!assignment.getReviewer().getUsername().equals(reviewerUsername)) {
            throw new SecurityException("Не ваше назначение");
        }
        return reviewRepository.findByAssignmentId(assignmentId).orElseGet(() -> {
            Review r = new Review();
            r.setAssignment(assignment);
            r.setStatus(ReviewStatus.DRAFT);
            return reviewRepository.save(r);
        });
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Review> findByAssignment(UUID assignmentId) {
        return reviewRepository.findByAssignmentId(assignmentId);
    }

    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> currentScores(UUID reviewId) {
        Map<UUID, BigDecimal> out = new HashMap<>();
        for (ReviewScore s : scoreRepository.findAllByReviewId(reviewId)) {
            out.put(s.getCriteria().getId(), s.getScore());
        }
        return out;
    }

    @Transactional
    public Review saveDraft(UUID assignmentId,
                            String username,
                            String feedback,
                            Map<UUID, BigDecimal> scoresByCriterion) {
        Review review = getOrCreateDraft(assignmentId, username);
        review.setFeedback(feedback);
        review.setStatus(ReviewStatus.DRAFT);
        applyScores(review, scoresByCriterion);
        review.setTotalScore(computeTotalScore(review));
        return reviewRepository.save(review);
    }

    @Transactional
    public Review submitFinal(UUID assignmentId,
                              String username,
                              String feedback,
                              Map<UUID, BigDecimal> scoresByCriterion) {
        Review review = saveDraft(assignmentId, username, feedback, scoresByCriterion);
        review.setStatus(ReviewStatus.FINAL);
        reviewRepository.save(review);
        WorkAssignment a = review.getAssignment();
        a.setStatus(AssignmentStatus.COMPLETED);
        a.setCompletedAt(LocalDateTime.now());
        assignmentRepository.save(a);
        Work w = a.getWork();
        w.setStatus(WorkStatus.REVIEWED);
        workRepository.save(w);
        return review;
    }

    @Transactional
    public WorkAssignment reopen(UUID assignmentId, String reviewerUsername) {
        WorkAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Назначение не найдено"));
        if (!assignment.getReviewer().getUsername().equals(reviewerUsername)) {
            throw new SecurityException("Не ваше назначение");
        }
        if (assignment.getStatus() != AssignmentStatus.COMPLETED) {
            throw new IllegalStateException("Можно открыть только завершённую рецензию");
        }
        Review r = reviewRepository.findByAssignmentId(assignmentId).orElse(null);
        if (r != null) {
            r.setStatus(ReviewStatus.DRAFT);
            reviewRepository.save(r);
        }
        assignment.setStatus(AssignmentStatus.IN_PROGRESS);
        assignment.setCompletedAt(null);
        assignmentRepository.save(assignment);
        Work w = assignment.getWork();
        w.setStatus(WorkStatus.UNDER_REVIEW);
        workRepository.save(w);
        return assignment;
    }

    @Transactional
    public Work sendBackForRevision(UUID assignmentId, String reviewerUsername, String comment) {
        WorkAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Назначение не найдено"));
        if (!assignment.getReviewer().getUsername().equals(reviewerUsername)) {
            throw new SecurityException("Не ваше назначение");
        }
        Review review = getOrCreateDraft(assignmentId, reviewerUsername);
        if (comment != null && !comment.isBlank()) {
            review.setFeedback(comment);
            reviewRepository.save(review);
        }
        assignment.setStatus(AssignmentStatus.ABANDONED);
        assignmentRepository.save(assignment);
        Work w = assignment.getWork();
        w.setStatus(WorkStatus.NEEDS_REVISION);
        return workRepository.save(w);
    }

    @Transactional
    public ReviewAttachment attachVoice(UUID reviewId, byte[] audio, Long durationMs) throws IOException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Отзыв не найден"));
        String folder = "reviews/" + reviewId + "/voice";
        FileStorageService.StoredFile stored = storage.storeVoice(audio, folder);
        ReviewAttachment att = new ReviewAttachment();
        att.setReview(review);
        att.setKind(com.normilinet.otklik.domain.enums.FileKind.AUDIO);
        att.setOriginalFilename("voice.webm");
        att.setStoredPath(stored.relativePath());
        att.setMimeType("audio/webm");
        att.setSizeBytes(stored.size());
        att.setVoice(true);
        att.setDurationMs(durationMs);
        return attachmentRepository.save(att);
    }

    @Transactional
    public ReviewAttachment attachLink(UUID reviewId, String url) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Отзыв не найден"));
        ReviewAttachment att = new ReviewAttachment();
        att.setReview(review);
        att.setKind(com.normilinet.otklik.domain.enums.FileKind.LINK);
        att.setExternalUrl(url);
        return attachmentRepository.save(att);
    }

    @Transactional(readOnly = true)
    public List<ReviewAttachment> getAttachments(UUID reviewId) {
        return attachmentRepository.findAllByReviewIdOrderByCreatedAtAsc(reviewId);
    }

    private void applyScores(Review review, Map<UUID, BigDecimal> scoresByCriterion) {
        if (scoresByCriterion == null) return;
        Map<UUID, ReviewScore> existing = new HashMap<>();
        for (ReviewScore s : scoreRepository.findAllByReviewId(review.getId())) {
            existing.put(s.getCriteria().getId(), s);
        }
        for (Map.Entry<UUID, BigDecimal> e : scoresByCriterion.entrySet()) {
            UUID critId = e.getKey();
            BigDecimal value = e.getValue();
            if (value == null) continue;
            ReviewScore rs = existing.get(critId);
            if (rs == null) {
                rs = new ReviewScore();
                rs.setReview(review);
                EvaluationCriterion c = criterionRepository.findById(critId)
                        .orElseThrow(() -> new IllegalArgumentException("Критерий не найден"));
                rs.setCriteria(c);
            }
            rs.setScore(value.setScale(2, RoundingMode.HALF_UP));
            scoreRepository.save(rs);
        }
    }

    private BigDecimal computeTotalScore(Review review) {
        List<ReviewScore> scores = scoreRepository.findAllByReviewId(review.getId());
        if (scores.isEmpty()) return null;
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;
        for (ReviewScore s : scores) {
            BigDecimal w = s.getCriteria().getWeight();
            if (w == null) w = BigDecimal.ZERO;
            weighted = weighted.add(s.getScore().multiply(w));
            weightSum = weightSum.add(w);
        }
        if (weightSum.compareTo(BigDecimal.ZERO) == 0) return null;
        return weighted.divide(weightSum, 2, RoundingMode.HALF_UP);
    }
}
