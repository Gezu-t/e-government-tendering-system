package com.egov.tendering.tender.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "pre_bid_clarifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreBidClarification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tender_id", nullable = false)
    private Long tenderId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "asked_by", nullable = false)
    private Long askedBy;

    @Column(name = "asked_by_org_name", length = 200)
    private String askedByOrgName;

    @Column(name = "answered_by")
    private Long answeredBy;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private ClarificationStatus status;

    @Column(name = "asked_at", nullable = false, updatable = false)
    private LocalDateTime askedAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @PrePersist
    protected void onCreate() {
        askedAt = LocalDateTime.now();
        if (status == null) {
            status = ClarificationStatus.PENDING;
        }
        if (isPublic == null) {
            isPublic = true;
        }
    }

    public enum ClarificationStatus {
        PENDING,
        ANSWERED,
        REJECTED
    }
}
