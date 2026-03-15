package com.egov.tendering.contract.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_performances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "tender_id", nullable = false)
    private Long tenderId;

    @Column(name = "quality_score", precision = 5, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "timeliness_score", precision = 5, scale = 2)
    private BigDecimal timelinessScore;

    @Column(name = "compliance_score", precision = 5, scale = 2)
    private BigDecimal complianceScore;

    @Column(name = "communication_score", precision = 5, scale = 2)
    private BigDecimal communicationScore;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "milestones_completed")
    private Integer milestonesCompleted;

    @Column(name = "milestones_total")
    private Integer milestonesTotal;

    @Column(name = "milestones_on_time")
    private Integer milestonesOnTime;

    @Column(name = "penalties_count")
    private Integer penaltiesCount;

    @Column(name = "penalty_amount", precision = 15, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "review_period", length = 50)
    private String reviewPeriod;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
