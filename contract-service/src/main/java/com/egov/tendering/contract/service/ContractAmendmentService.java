package com.egov.tendering.contract.service;

import com.egov.tendering.contract.dal.dto.ContractAmendmentDTO;
import com.egov.tendering.contract.dal.dto.ContractAmendmentRequest;

import java.util.List;

public interface ContractAmendmentService {

    ContractAmendmentDTO requestAmendment(Long contractId, ContractAmendmentRequest request, Long requestedBy);

    ContractAmendmentDTO approveAmendment(Long amendmentId, Long approvedBy);

    ContractAmendmentDTO rejectAmendment(Long amendmentId, Long rejectedBy);

    List<ContractAmendmentDTO> getAmendmentsByContract(Long contractId);

    List<ContractAmendmentDTO> getPendingAmendments(Long contractId);
}
