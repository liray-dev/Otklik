package com.normilinet.otklik.domain.repository;

import com.normilinet.otklik.domain.model.ReviewScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewScoreRepository extends JpaRepository<ReviewScore, UUID> {
    List<ReviewScore> findAllByReviewId(UUID reviewId);
}
