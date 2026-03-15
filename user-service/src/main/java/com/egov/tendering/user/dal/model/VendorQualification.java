package com.egov.tendering.user.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_qualifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorQualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "qualification_category", nullable = false, length = 100)
    private String qualificationCategory;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private QualificationStatus status;

    @Column(name = "business_license_number", length = 100)
    private String businessLicenseNumber;

    @Column(name = "tax_registration_number", length = 100)
    private String taxRegistrationNumber;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "annual_revenue", length = 50)
    private String annualRevenue;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "past_contracts_count")
    private Integer pastContractsCount;

    @Column(name = "certification_details", columnDefinition = "TEXT")
    private String certificationDetails;

    @Column(name = "financial_statement_path", length = 500)
    private String financialStatementPath;

    @Column(name = "qualification_score")
    private Integer qualificationScore;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = QualificationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
