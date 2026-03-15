package com.egov.tendering.evaluation.dal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiCriteriaEvaluationResult {
    private Long evaluationId;
    private Long bidId;
    private String bidderName;
    private BigDecimal overallScore;
    private List<EvaluationScoreSummaryDTO> categoryBreakdown;
    private boolean allMandatoryCategoriesPassed;
    private boolean qualified;
}
