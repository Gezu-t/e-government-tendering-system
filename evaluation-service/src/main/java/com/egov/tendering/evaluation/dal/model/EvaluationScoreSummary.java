package com.egov.tendering.evaluation.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_score_summaries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationScoreSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ScoreCategory category;

    @Column(name = "category_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal categoryWeight;

    @Column(name = "raw_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal rawScore;

    @Column(name = "weighted_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal weightedScore;

    @Column(name = "max_possible_score", precision = 10, scale = 2)
    private BigDecimal maxPossibleScore;

    @Column(name = "criteria_count")
    private Integer criteriaCount;

    @Column(name = "pass_threshold", precision = 5, scale = 2)
    private BigDecimal passThreshold;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
