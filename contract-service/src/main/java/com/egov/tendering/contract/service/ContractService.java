package com.egov.tendering.contract.service;

import com.egov.tendering.contract.dal.dto.ContractDTO;
import com.egov.tendering.contract.dal.dto.CreateContractRequest;
import com.egov.tendering.contract.dal.model.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ContractService {

  ContractDTO createContract(CreateContractRequest request, String username);

  ContractDTO getContractById(Long contractId);

  List<ContractDTO> getContractsByTenderId(Long tenderId);

  Page<ContractDTO> getContractsByBidderId(Long bidderId, Pageable pageable);

  Page<ContractDTO> searchContracts(String title, ContractStatus status, Long tenderId, Long bidderId,
                                    String username, boolean admin, Pageable pageable);

  ContractDTO updateContractStatus(Long contractId, ContractStatus newStatus, String username);

  ContractDTO activateContract(Long contractId, String username);

  ContractDTO completeContract(Long contractId, String username);

  ContractDTO terminateContract(Long contractId, String reason, String username);

  void checkForCompletedContracts();
}
