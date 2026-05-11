package com.normilinet.otklik.domain.model;

import com.normilinet.otklik.domain.enums.FileKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "campaign_attachments")
@Getter
@Setter
public class CampaignAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FileKind kind;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "stored_path", length = 1000)
    private String storedPath;

    @Column(name = "mime_type", length = 200)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "external_url", length = 1000)
    private String externalUrl;
}
