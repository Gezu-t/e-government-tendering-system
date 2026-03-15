package com.egov.tendering.evaluation.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "conflict_of_interest_declarations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictOfInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tender_id", nullable = false)
    private Long tenderId;

    @Column(name = "evaluator_id", nullable = false)
    private Long evaluatorId;

    @Column(name = "has_conflict", nullable = false)
    private Boolean hasConflict;

    @Column(name = "conflict_description", columnDefinition = "TEXT")
    private String conflictDescription;

    @Column(name = "related_organization_id")
    private Long relatedOrganizationId;

    @Column(name = "relationship_type", length = 100)
    private String relationshipType;

    @Column(name = "declaration_text", columnDefinition = "TEXT", nullable = false)
    private String declarationText;

    @Column(name = "acknowledged", nullable = false)
    private Boolean acknowledged;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "review_decision", length = 30)
    private String reviewDecision;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "declared_at", nullable = false, updatable = false)
    private LocalDateTime declaredAt;

    @PrePersist
    protected void onCreate() {
        declaredAt = LocalDateTime.now();
        if (acknowledged == null) acknowledged = false;
    }
}
