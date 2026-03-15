package com.egov.tendering.bidding.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "bid_seals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidSeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bid_id", nullable = false, unique = true)
    private Long bidId;

    @Column(name = "tender_id", nullable = false)
    private Long tenderId;

    @Column(name = "content_hash", nullable = false, length = 512)
    private String contentHash;

    @Column(name = "encrypted_content", columnDefinition = "LONGTEXT")
    private String encryptedContent;

    @Column(name = "encryption_algorithm", nullable = false, length = 50)
    private String encryptionAlgorithm;

    @Column(name = "seal_key_reference", length = 255)
    private String sealKeyReference;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private SealStatus status;

    @Column(name = "sealed_at", nullable = false)
    private LocalDateTime sealedAt;

    @Column(name = "sealed_by", nullable = false)
    private Long sealedBy;

    @Column(name = "unsealed_at")
    private LocalDateTime unsealedAt;

    @Column(name = "unsealed_by")
    private Long unsealedBy;

    @Column(name = "scheduled_unseal_time", nullable = false)
    private LocalDateTime scheduledUnsealTime;

    @Column(name = "integrity_verified")
    private Boolean integrityVerified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = SealStatus.SEALED;
        }
    }
}
