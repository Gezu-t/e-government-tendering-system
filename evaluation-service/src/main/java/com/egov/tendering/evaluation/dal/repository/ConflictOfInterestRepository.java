package com.egov.tendering.evaluation.dal.repository;

import com.egov.tendering.evaluation.dal.model.ConflictOfInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConflictOfInterestRepository extends JpaRepository<ConflictOfInterest, Long> {

    List<ConflictOfInterest> findByTenderId(Long tenderId);

    Optional<ConflictOfInterest> findByTenderIdAndEvaluatorId(Long tenderId, Long evaluatorId);

    List<ConflictOfInterest> findByTenderIdAndHasConflictTrue(Long tenderId);

    boolean existsByTenderIdAndEvaluatorId(Long tenderId, Long evaluatorId);
}
