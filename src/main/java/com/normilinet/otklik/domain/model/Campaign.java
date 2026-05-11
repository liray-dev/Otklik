package com.normilinet.otklik.domain.model;

import com.normilinet.otklik.domain.enums.AnonymityMode;
import com.normilinet.otklik.domain.enums.CampaignMode;
import com.normilinet.otklik.domain.enums.CampaignStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
public class Campaign extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CampaignMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "anonymity_mode", nullable = false, length = 50)
    private AnonymityMode anonymityMode = AnonymityMode.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CampaignStatus status;

    @Column(name = "scale_max", nullable = false)
    private int scaleMax = 10;

    @Column(name = "expected_duration_days")
    private Integer expectedDurationDays;

    @Column
    private LocalDateTime deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    private User organizer;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "campaign_tags",
            joinColumns = @JoinColumn(name = "campaign_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();
}
