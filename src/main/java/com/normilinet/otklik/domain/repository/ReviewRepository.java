package com.normilinet.otklik.domain.repository;

import com.normilinet.otklik.domain.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
}
