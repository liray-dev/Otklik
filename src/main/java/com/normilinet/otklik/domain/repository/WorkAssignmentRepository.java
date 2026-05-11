package com.normilinet.otklik.domain.repository;

import com.normilinet.otklik.domain.enums.AssignmentStatus;
import com.normilinet.otklik.domain.model.WorkAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkAssignmentRepository extends JpaRepository<WorkAssignment, UUID> {
    List<WorkAssignment> findAllByReviewerIdOrderByCreatedAtDesc(UUID reviewerId);
    List<WorkAssignment> findAllByWorkId(UUID workId);
    List<WorkAssignment> findAllByReviewerIdAndStatus(UUID reviewerId, AssignmentStatus status);
    long countByWorkId(UUID workId);
}
