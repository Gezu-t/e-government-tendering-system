package com.egov.tendering.evaluation.service;

import com.egov.tendering.evaluation.dal.dto.EvaluationCategoryConfigDTO;
import com.egov.tendering.evaluation.dal.dto.EvaluationScoreSummaryDTO;
import com.egov.tendering.evaluation.dal.dto.MultiCriteriaEvaluationResult;

import java.util.List;

public interface MultiCriteriaEvaluationService {

    /**
     * Configures scoring categories (Technical, Financial, Compliance, etc.) for a tender
     * with weights and pass thresholds.
     */
    List<EvaluationCategoryConfigDTO> configureCategories(Long tenderId, List<EvaluationCategoryConfigDTO> configs);

    /**
     * Gets the category configuration for a tender.
     */
    List<EvaluationCategoryConfigDTO> getCategoryConfigs(Long tenderId);

    /**
     * Computes the multi-criteria score breakdown for an evaluation,
     * grouping criteria scores by category and calculating weighted totals.
     */
    List<EvaluationScoreSummaryDTO> computeScoreBreakdown(Long evaluationId);

    /**
     * Gets the full multi-criteria evaluation result for a bid including
     * category breakdowns and pass/fail status.
     */
    MultiCriteriaEvaluationResult getMultiCriteriaResult(Long evaluationId);

    /**
     * Gets multi-criteria results for all bids in a tender.
     */
    List<MultiCriteriaEvaluationResult> getMultiCriteriaResultsForTender(Long tenderId);
}
