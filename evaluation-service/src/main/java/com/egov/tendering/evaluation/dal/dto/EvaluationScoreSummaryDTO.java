package com.egov.tendering.evaluation.dal.dto;

import com.egov.tendering.evaluation.dal.model.ScoreCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationScoreSummaryDTO {
    private Long id;
    private Long evaluationId;
    private ScoreCategory category;
    private BigDecimal categoryWeight;
    private BigDecimal rawScore;
    private BigDecimal weightedScore;
    private BigDecimal maxPossibleScore;
    private Integer criteriaCount;
    private BigDecimal passThreshold;
    private Boolean passed;
}
