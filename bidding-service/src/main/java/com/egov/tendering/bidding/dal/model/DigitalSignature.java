package com.egov.tendering.bidding.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "digital_signatures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "signer_id", nullable = false)
    private Long signerId;

    @Column(name = "signer_name", nullable = false)
    private String signerName;

    @Column(name = "signature_value", columnDefinition = "TEXT", nullable = false)
    private String signatureValue;

    @Column(name = "content_hash", nullable = false, length = 512)
    private String contentHash;

    @Column(name = "algorithm", nullable = false, length = 50)
    private String algorithm;

    @Column(name = "certificate_serial", length = 255)
    private String certificateSerial;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SignatureStatus status;

    @Column(name = "signed_at", nullable = false)
    private LocalDateTime signedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = SignatureStatus.PENDING_VERIFICATION;
        }
    }
}
