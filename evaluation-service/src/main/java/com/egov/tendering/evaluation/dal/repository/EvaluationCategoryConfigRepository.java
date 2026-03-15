package com.egov.tendering.evaluation.dal.repository;

import com.egov.tendering.evaluation.dal.model.EvaluationCategoryConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationCategoryConfigRepository extends JpaRepository<EvaluationCategoryConfig, Long> {

    List<EvaluationCategoryConfig> findByTenderId(Long tenderId);

    void deleteByTenderId(Long tenderId);
}
