package com.egov.tendering.bidding.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bid_submission_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidSubmissionMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bid_id", nullable = false)
    private Long bidId;

    @Column(name = "tender_id", nullable = false)
    private Long tenderId;

    @Column(name = "tenderer_id", nullable = false)
    private Long tendererId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "geo_location", length = 100)
    private String geoLocation;

    @Column(name = "submission_time", nullable = false)
    private LocalDateTime submissionTime;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "flagged")
    private Boolean flagged;

    @Column(name = "flag_reason")
    private String flagReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (flagged == null) {
            flagged = false;
        }
    }
}
