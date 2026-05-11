package com.normilinet.otklik.domain.model;

import com.normilinet.otklik.domain.enums.WorkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "works")
@Getter
@Setter
public class Work extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private String title;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "external_link", length = 1000)
    private String externalLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WorkStatus status;

    @Version
    private Long version;
}
