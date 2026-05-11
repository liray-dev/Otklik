package com.normilinet.otklik.domain.repository;

import com.normilinet.otklik.domain.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);
    List<Tag> findAllByOrderByNameAsc();
}
