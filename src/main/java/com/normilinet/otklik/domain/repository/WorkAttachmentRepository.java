package com.normilinet.otklik.domain.repository;

import com.normilinet.otklik.domain.model.WorkAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkAttachmentRepository extends JpaRepository<WorkAttachment, UUID> {
    List<WorkAttachment> findAllByWorkIdOrderByCreatedAtAsc(UUID workId);
}
