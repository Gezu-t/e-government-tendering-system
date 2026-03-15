package com.egov.tendering.contract.service;

import com.egov.tendering.contract.dal.dto.ContractMilestoneDTO;
import java.util.List;

public interface MilestoneService {

    List<ContractMilestoneDTO> getMilestonesByContractId(Long contractId);

    ContractMilestoneDTO addMilestone(Long contractId, ContractMilestoneDTO milestoneDTO, String username);

    ContractMilestoneDTO completeMilestone(Long contractId, Long milestoneId, String username);

    void checkForOverdueMilestones();
}
