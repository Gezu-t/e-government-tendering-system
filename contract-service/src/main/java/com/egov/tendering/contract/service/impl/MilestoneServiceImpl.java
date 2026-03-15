package com.egov.tendering.contract.service.impl;

import com.egov.tendering.contract.dal.dto.ContractMilestoneDTO;
import com.egov.tendering.contract.dal.mapper.ContractMapper;
import com.egov.tendering.contract.dal.model.Contract;
import com.egov.tendering.contract.dal.model.ContractMilestone;
import com.egov.tendering.contract.dal.model.ContractStatus;
import com.egov.tendering.contract.dal.model.MilestoneStatus;
import com.egov.tendering.contract.dal.repository.ContractMilestoneRepository;
import com.egov.tendering.contract.dal.repository.ContractRepository;
import com.egov.tendering.contract.event.ContractEventPublisher;
import com.egov.tendering.contract.exception.ContractNotFoundException;
import com.egov.tendering.contract.exception.MilestoneNotFoundException;
import com.egov.tendering.contract.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilestoneServiceImpl implements MilestoneService {

    private final ContractRepository contractRepository;
    private final ContractMilestoneRepository milestoneRepository;
    private final ContractMapper contractMapper;
    private final ContractEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<ContractMilestoneDTO> getMilestonesByContractId(Long contractId) {
        log.info("Retrieving milestones for contract ID: {}", contractId);

        Objects.requireNonNull(contractId, "Contract ID cannot be null");

        // Optimize: Fetch directly instead of checking existence first
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException("Contract not found with ID: " + contractId));

        List<ContractMilestone> milestones = milestoneRepository.findByContractId(contractId);
        return contractMapper.toMilestoneDtoList(milestones);
    }

    @Override
    @Transactional
    public ContractMilestoneDTO addMilestone(Long contractId, ContractMilestoneDTO milestoneDTO, String username) {
        log.info("Adding milestone to contract ID: {}", contractId);

        Objects.requireNonNull(contractId, "Contract ID cannot be null");
        Objects.requireNonNull(milestoneDTO, "Milestone DTO cannot be null");
        Objects.requireNonNull(username, "Username cannot be null");

        // Validate milestone data
        if (milestoneDTO.getDueDate() == null) {
            throw new IllegalArgumentException("Milestone due date cannot be null");
        }
        if (milestoneDTO.getPaymentAmount() != null && milestoneDTO.getPaymentAmount().signum() < 0) {
            throw new IllegalArgumentException("Payment amount cannot be negative");
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException("Contract not found with ID: " + contractId));

        validateMilestoneCanBeManaged(contract);
        validateMilestoneDueDate(contract, milestoneDTO.getDueDate());
        validateMilestoneBudget(contract, milestoneDTO.getPaymentAmount(), null);

        ContractMilestone milestone = contractMapper.toMilestoneEntity(milestoneDTO);
        milestone.setContract(contract);
        milestone.setStatus(MilestoneStatus.PENDING);

        milestone = milestoneRepository.save(milestone);

        // Update the contract's milestones list (if not relying solely on JPA cascade)
        contract.getMilestones().add(milestone);

        try {
            eventPublisher.publishMilestoneAddedEvent(contract, milestone);
        } catch (Exception e) {
            log.error("Failed to publish milestone added event for milestone ID: {}", milestone.getId(), e);
        }

        return contractMapper.toMilestoneDto(milestone);
    }

    @Override
    @Transactional
    public ContractMilestoneDTO completeMilestone(Long contractId, Long milestoneId, String username) {
        log.info("Completing milestone with ID: {}", milestoneId);

        Objects.requireNonNull(contractId, "Contract ID cannot be null");
        Objects.requireNonNull(milestoneId, "Milestone ID cannot be null");
        Objects.requireNonNull(username, "Username cannot be null");

        ContractMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException("Milestone not found with ID: " + milestoneId));

        if (!contractId.equals(milestone.getContract().getId())) {
            throw new IllegalArgumentException("Milestone does not belong to contract " + contractId);
        }

        if (milestone.getContract().getStatus() != ContractStatus.ACTIVE) {
            throw new IllegalStateException("Only milestones for ACTIVE contracts can be completed");
        }

        if (milestone.getStatus() != MilestoneStatus.PENDING && milestone.getStatus() != MilestoneStatus.OVERDUE) {
            throw new IllegalStateException("Only pending or overdue milestones can be completed");
        }

        milestone.setStatus(MilestoneStatus.COMPLETED);
        milestone.setCompletedDate(LocalDate.now()); // Assumes this field exists in ContractMilestone

        milestone = milestoneRepository.save(milestone);

        Contract contract = milestone.getContract();
        try {
            eventPublisher.publishMilestoneCompletedEvent(contract, milestone);
        } catch (Exception e) {
            log.error("Failed to publish milestone completed event for milestone ID: {}", milestoneId, e);
        }

        return contractMapper.toMilestoneDto(milestone);
    }

    @Override
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void checkForOverdueMilestones() {
        log.info("Checking for overdue milestones");

        LocalDate today = LocalDate.now();
        List<ContractMilestone> overdueMilestones = milestoneRepository.findByStatusAndDueDateBefore(
                MilestoneStatus.PENDING, today);

        int updatedCount = 0;
        for (ContractMilestone milestone : overdueMilestones) {
            if (milestone.getContract().getStatus() != ContractStatus.ACTIVE) {
                continue;
            }
            log.info("Marking milestone {} as overdue", milestone.getId());
            milestone.setStatus(MilestoneStatus.OVERDUE);
            milestoneRepository.save(milestone);

            Contract contract = milestone.getContract();
            try {
                eventPublisher.publishMilestoneOverdueEvent(contract, milestone);
            } catch (Exception e) {
                log.error("Failed to publish milestone overdue event for milestone ID: {}", milestone.getId(), e);
            }
            updatedCount++;
        }

        log.info("Marked {} milestones as overdue", updatedCount);
    }

    private void validateMilestoneCanBeManaged(Contract contract) {
        if (contract.getStatus() == ContractStatus.COMPLETED
                || contract.getStatus() == ContractStatus.TERMINATED
                || contract.getStatus() == ContractStatus.CANCELLED) {
            throw new IllegalStateException("Milestones cannot be changed for contracts in status " + contract.getStatus());
        }
    }

    private void validateMilestoneDueDate(Contract contract, LocalDate dueDate) {
        if (dueDate.isBefore(contract.getStartDate()) || dueDate.isAfter(contract.getEndDate())) {
            throw new IllegalArgumentException("Milestone due date must fall within the contract start and end dates");
        }
    }

    private void validateMilestoneBudget(Contract contract, BigDecimal paymentAmount, Long milestoneIdToExclude) {
        BigDecimal existingBudget = milestoneRepository.findByContractId(contract.getId()).stream()
                .filter(milestone -> milestoneIdToExclude == null || !milestoneIdToExclude.equals(milestone.getId()))
                .map(ContractMilestone::getPaymentAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal requestedPayment = paymentAmount == null ? BigDecimal.ZERO : paymentAmount;
        BigDecimal totalMilestoneBudget = existingBudget.add(requestedPayment);
        if (totalMilestoneBudget.compareTo(contract.getTotalValue()) > 0) {
            throw new IllegalArgumentException("Total milestone payments cannot exceed the contract total value");
        }
    }
}
