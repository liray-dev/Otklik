package com.normilinet.otklik.domain.model;

import com.normilinet.otklik.domain.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "invites")
@Getter
@Setter
public class Invite extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @Column(name = "usages_limit", nullable = false)
    private int usagesLimit;

    @Column(name = "activations_count", nullable = false)
    private int activationsCount = 0;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
