package com.egov.tendering.contract.service.impl;

import com.egov.tendering.contract.dal.dto.ContractAmendmentDTO;
import com.egov.tendering.contract.dal.dto.ContractAmendmentRequest;
import com.egov.tendering.contract.dal.model.Contract;
import com.egov.tendering.contract.dal.model.ContractAmendment;
import com.egov.tendering.contract.dal.model.ContractAmendment.AmendmentStatus;
import com.egov.tendering.contract.dal.model.ContractAmendment.AmendmentType;
import com.egov.tendering.contract.dal.repository.ContractAmendmentRepository;
import com.egov.tendering.contract.dal.repository.ContractRepository;
import com.egov.tendering.contract.service.ContractAmendmentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractAmendmentServiceImpl implements ContractAmendmentService {

    private static final EnumSet<com.egov.tendering.contract.dal.model.ContractStatus> AMENDABLE_STATUSES =
            EnumSet.of(com.egov.tendering.contract.dal.model.ContractStatus.ACTIVE);

    private final ContractAmendmentRepository amendmentRepository;
    private final ContractRepository contractRepository;

    @Override
    @Transactional
    public ContractAmendmentDTO requestAmendment(Long contractId, ContractAmendmentRequest request, Long requestedBy) {
        log.info("Requesting contract amendment for contract {} by user {}", contractId, requestedBy);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found: " + contractId));
        validateContractCanBeAmended(contract);
        validateRequest(contract, request);

        Integer nextNumber = amendmentRepository.findMaxAmendmentNumber(contractId) + 1;

        ContractAmendment amendment = ContractAmendment.builder()
                .contractId(contractId)
                .amendmentNumber(nextNumber)
                .type(request.getType())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(AmendmentStatus.PENDING)
                .requestedBy(requestedBy)
                .build();

        if (request.getType() == AmendmentType.VALUE_CHANGE) {
            amendment.setPreviousValue(contract.getTotalValue());
            amendment.setNewValue(request.getNewValue());
        }
        if (request.getType() == AmendmentType.TIMELINE_EXTENSION) {
            amendment.setPreviousEndDate(contract.getEndDate());
            amendment.setNewEndDate(request.getNewEndDate());
        }

        amendment = amendmentRepository.save(amendment);
        log.info("Contract amendment #{} created for contract {}", nextNumber, contractId);

        return toDTO(amendment);
    }

    @Override
    @Transactional
    public ContractAmendmentDTO approveAmendment(Long amendmentId, Long approvedBy) {
        log.info("Approving contract amendment {} by user {}", amendmentId, approvedBy);

        ContractAmendment amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new EntityNotFoundException("Amendment not found: " + amendmentId));
        validatePendingAmendment(amendment, "approved");

        amendment.setStatus(AmendmentStatus.APPROVED);
        amendment.setApprovedBy(approvedBy);
        amendment.setApprovedAt(LocalDateTime.now());

        // Apply the amendment to the contract
        Contract contract = contractRepository.findById(amendment.getContractId())
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));
        validateContractCanBeAmended(contract);

        if (amendment.getNewValue() != null) {
            contract.setTotalValue(amendment.getNewValue());
        }
        if (amendment.getNewEndDate() != null) {
            contract.setEndDate(amendment.getNewEndDate());
        }
        contractRepository.save(contract);

        amendment = amendmentRepository.save(amendment);
        log.info("Contract amendment {} approved and applied", amendmentId);

        return toDTO(amendment);
    }

    @Override
    @Transactional
    public ContractAmendmentDTO rejectAmendment(Long amendmentId, Long rejectedBy) {
        ContractAmendment amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new EntityNotFoundException("Amendment not found: " + amendmentId));
        validatePendingAmendment(amendment, "rejected");

        amendment.setStatus(AmendmentStatus.REJECTED);
        amendment.setApprovedBy(rejectedBy);
        amendment.setApprovedAt(LocalDateTime.now());
        amendment = amendmentRepository.save(amendment);

        return toDTO(amendment);
    }

    @Override
    public List<ContractAmendmentDTO> getAmendmentsByContract(Long contractId) {
        return amendmentRepository.findByContractIdOrderByAmendmentNumberDesc(contractId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<ContractAmendmentDTO> getPendingAmendments(Long contractId) {
        return amendmentRepository.findByContractIdAndStatus(contractId, AmendmentStatus.PENDING)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private ContractAmendmentDTO toDTO(ContractAmendment a) {
        return ContractAmendmentDTO.builder()
                .id(a.getId()).contractId(a.getContractId()).amendmentNumber(a.getAmendmentNumber())
                .type(a.getType()).reason(a.getReason()).description(a.getDescription())
                .previousValue(a.getPreviousValue()).newValue(a.getNewValue())
                .previousEndDate(a.getPreviousEndDate()).newEndDate(a.getNewEndDate())
                .status(a.getStatus()).requestedBy(a.getRequestedBy())
                .approvedBy(a.getApprovedBy()).approvedAt(a.getApprovedAt())
                .createdAt(a.getCreatedAt()).build();
    }

    private void validateContractCanBeAmended(Contract contract) {
        if (!AMENDABLE_STATUSES.contains(contract.getStatus())) {
            throw new IllegalStateException("Only active contracts can be amended");
        }
    }

    private void validatePendingAmendment(ContractAmendment amendment, String action) {
        if (amendment.getStatus() != AmendmentStatus.PENDING) {
            throw new IllegalStateException("Only pending amendments can be " + action);
        }
    }

    private void validateRequest(Contract contract, ContractAmendmentRequest request) {
        switch (request.getType()) {
            case VALUE_CHANGE -> {
                if (request.getNewValue() == null) {
                    throw new IllegalArgumentException("A value change amendment requires a new contract value");
                }
                if (request.getNewValue().signum() < 0) {
                    throw new IllegalArgumentException("Contract value cannot be negative");
                }
                if (request.getNewValue().compareTo(contract.getTotalValue()) == 0) {
                    throw new IllegalArgumentException("Contract value amendment must change the current value");
                }
            }
            case TIMELINE_EXTENSION -> {
                if (request.getNewEndDate() == null) {
                    throw new IllegalArgumentException("A timeline extension amendment requires a new end date");
                }
                if (!request.getNewEndDate().isAfter(contract.getEndDate())) {
                    throw new IllegalArgumentException("Timeline extension must move the contract end date forward");
                }
            }
            default -> {
                if (request.getDescription() == null || request.getDescription().isBlank()) {
                    throw new IllegalArgumentException("This amendment type requires a descriptive change summary");
                }
            }
        }
    }
}
