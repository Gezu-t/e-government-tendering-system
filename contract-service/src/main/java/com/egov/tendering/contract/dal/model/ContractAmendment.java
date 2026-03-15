package com.egov.tendering.contract.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_amendments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractAmendment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "amendment_number", nullable = false)
    private Integer amendmentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private AmendmentType type;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "previous_value", precision = 15, scale = 2)
    private BigDecimal previousValue;

    @Column(name = "new_value", precision = 15, scale = 2)
    private BigDecimal newValue;

    @Column(name = "previous_end_date")
    private LocalDate previousEndDate;

    @Column(name = "new_end_date")
    private LocalDate newEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AmendmentStatus status;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = AmendmentStatus.PENDING;
    }

    public enum AmendmentType {
        VALUE_CHANGE,
        SCOPE_CHANGE,
        TIMELINE_EXTENSION,
        PENALTY_WAIVER,
        TERMINATION_CLAUSE,
        OTHER
    }

    public enum AmendmentStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
