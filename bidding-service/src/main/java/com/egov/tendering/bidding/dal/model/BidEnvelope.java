package com.egov.tendering.bidding.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bid_envelopes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidEnvelope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bid_id", nullable = false)
    private Long bidId;

    @Enumerated(EnumType.STRING)
    @Column(name = "envelope_type", nullable = false, length = 30)
    private EnvelopeType envelopeType;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "content_hash", length = 512)
    private String contentHash;

    @Column(name = "is_sealed", nullable = false)
    private Boolean isSealed;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "opened_by")
    private Long openedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isSealed == null) isSealed = true;
    }

    public enum EnvelopeType {
        TECHNICAL,
        FINANCIAL
    }
}
