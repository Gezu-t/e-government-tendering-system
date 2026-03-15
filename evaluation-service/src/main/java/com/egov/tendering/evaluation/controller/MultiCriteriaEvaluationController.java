package com.egov.tendering.evaluation.controller;

import com.egov.tendering.evaluation.dal.dto.EvaluationCategoryConfigDTO;
import com.egov.tendering.evaluation.dal.dto.EvaluationScoreSummaryDTO;
import com.egov.tendering.evaluation.dal.dto.MultiCriteriaEvaluationResult;
import com.egov.tendering.evaluation.service.MultiCriteriaEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/multi-criteria")
@RequiredArgsConstructor
@Slf4j
public class MultiCriteriaEvaluationController {

    private final MultiCriteriaEvaluationService multiCriteriaService;

    @PostMapping("/tenders/{tenderId}/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE')")
    public ResponseEntity<List<EvaluationCategoryConfigDTO>> configureCategories(
            @PathVariable Long tenderId,
            @Valid @RequestBody List<EvaluationCategoryConfigDTO> configs) {
        log.info("Configuring evaluation categories for tender: {}", tenderId);
        List<EvaluationCategoryConfigDTO> result = multiCriteriaService.configureCategories(tenderId, configs);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @GetMapping("/tenders/{tenderId}/categories")
    public ResponseEntity<List<EvaluationCategoryConfigDTO>> getCategoryConfigs(@PathVariable Long tenderId) {
        List<EvaluationCategoryConfigDTO> configs = multiCriteriaService.getCategoryConfigs(tenderId);
        return ResponseEntity.ok(configs);
    }

    @PostMapping("/evaluations/{evaluationId}/compute")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<List<EvaluationScoreSummaryDTO>> computeScoreBreakdown(
            @PathVariable Long evaluationId) {
        log.info("Computing score breakdown for evaluation: {}", evaluationId);
        List<EvaluationScoreSummaryDTO> result = multiCriteriaService.computeScoreBreakdown(evaluationId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/evaluations/{evaluationId}/result")
    public ResponseEntity<MultiCriteriaEvaluationResult> getMultiCriteriaResult(
            @PathVariable Long evaluationId) {
        MultiCriteriaEvaluationResult result = multiCriteriaService.getMultiCriteriaResult(evaluationId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/tenders/{tenderId}/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'COMMITTEE')")
    public ResponseEntity<List<MultiCriteriaEvaluationResult>> getResultsForTender(
            @PathVariable Long tenderId) {
        log.info("Getting multi-criteria results for tender: {}", tenderId);
        List<MultiCriteriaEvaluationResult> results = multiCriteriaService.getMultiCriteriaResultsForTender(tenderId);
        return ResponseEntity.ok(results);
    }
}
