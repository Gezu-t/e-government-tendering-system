package com.egov.tendering.contract.dal.repository;

import com.egov.tendering.contract.dal.model.ContractAmendment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContractAmendmentRepository extends JpaRepository<ContractAmendment, Long> {

    List<ContractAmendment> findByContractIdOrderByAmendmentNumberDesc(Long contractId);

    @Query("SELECT COALESCE(MAX(ca.amendmentNumber), 0) FROM ContractAmendment ca WHERE ca.contractId = :contractId")
    Integer findMaxAmendmentNumber(@Param("contractId") Long contractId);

    List<ContractAmendment> findByContractIdAndStatus(Long contractId, ContractAmendment.AmendmentStatus status);
}
