package com.egov.tendering.evaluation.dal.repository;

import com.egov.tendering.evaluation.dal.model.CommitteeApprovalPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommitteeApprovalPolicyRepository extends JpaRepository<CommitteeApprovalPolicy, Long> {

    Optional<CommitteeApprovalPolicy> findByTenderId(Long tenderId);
}
