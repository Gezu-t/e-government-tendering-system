package com.egov.tendering.evaluation.dal.repository;

import com.egov.tendering.evaluation.dal.model.EvaluationScoreSummary;
import com.egov.tendering.evaluation.dal.model.ScoreCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationScoreSummaryRepository extends JpaRepository<EvaluationScoreSummary, Long> {

    List<EvaluationScoreSummary> findByEvaluationId(Long evaluationId);

    Optional<EvaluationScoreSummary> findByEvaluationIdAndCategory(Long evaluationId, ScoreCategory category);

    void deleteByEvaluationId(Long evaluationId);
}
