package com.normilinet.otklik.domain.repository;

import com.normilinet.otklik.domain.enums.WorkStatus;
import com.normilinet.otklik.domain.model.Work;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkRepository extends JpaRepository<Work, UUID> {
    List<Work> findAllByCampaignId(UUID campaignId);
    List<Work> findAllByStudentIdOrderByCreatedAtDesc(UUID studentId);
    List<Work> findAllByCampaignIdAndStatus(UUID campaignId, WorkStatus status);
    List<Work> findAllByStatus(WorkStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Work w where w.id = :id")
    Optional<Work> findByIdForUpdate(@Param("id") UUID id);
}
