package com.egov.tendering.contract.service.impl;

import com.egov.tendering.contract.client.BidClient;
import com.egov.tendering.contract.dal.dto.ContractDTO;
import com.egov.tendering.contract.dal.dto.CreateContractRequest;
import com.egov.tendering.contract.dal.mapper.ContractMapper;
import com.egov.tendering.contract.dal.model.*;
import com.egov.tendering.contract.dal.repository.ContractItemRepository;
import com.egov.tendering.contract.dal.repository.ContractMilestoneRepository;
import com.egov.tendering.contract.dal.repository.ContractRepository;
import com.egov.tendering.contract.event.ContractEventPublisher;
import com.egov.tendering.contract.exception.ContractNotFoundException;
import com.egov.tendering.contract.service.ContractService;
import com.egov.tendering.dto.BidDTO;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractServiceImpl implements ContractService {

  private static final Map<ContractStatus, EnumSet<ContractStatus>> ALLOWED_MANUAL_STATUS_TRANSITIONS = Map.of(
          ContractStatus.DRAFT, EnumSet.of(ContractStatus.PENDING_SIGNATURE, ContractStatus.CANCELLED),
          ContractStatus.PENDING_SIGNATURE, EnumSet.of(ContractStatus.CANCELLED)
  );

  private final ContractRepository contractRepository;
  private final ContractItemRepository itemRepository;
  private final ContractMilestoneRepository milestoneRepository;
  private final ContractMapper contractMapper;
  private final ContractEventPublisher eventPublisher;
  private final BidClient bidClient;

  @Override
  @Transactional
  public ContractDTO createContract(CreateContractRequest request, String username) {
    log.info("Creating new contract for tender: {} and bidder: {}", request.getTenderId(), request.getBidderId());
    validateAwardedBidEligibility(request);

    // Calculate total value from items
    BigDecimal totalValue = request.getItems().stream()
            .map(item -> new BigDecimal(item.getQuantity()).multiply(item.getUnitPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Create contract
    Contract contract = Contract.builder()
            .tenderId(request.getTenderId())
            .bidderId(request.getBidderId())
            .contractNumber(request.getContractNumber())
            .title(request.getTitle())
            .description(request.getDescription())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .totalValue(totalValue)
            .status(ContractStatus.DRAFT)
            .createdBy(username)
            .build();

    // Save contract first to get ID
    contract = contractRepository.save(contract);

    // Create and add items
    Contract finalContract = contract;
    List<ContractItem> items = request.getItems().stream()
            .map(itemRequest -> {
              BigDecimal itemTotalPrice = new BigDecimal(itemRequest.getQuantity()).multiply(itemRequest.getUnitPrice());

              return ContractItem.builder()
                      .contract(finalContract)
                      .tenderItemId(itemRequest.getTenderItemId())
                      .name(itemRequest.getName())
                      .description(itemRequest.getDescription())
                      .quantity(itemRequest.getQuantity())
                      .unit(itemRequest.getUnit())
                      .unitPrice(itemRequest.getUnitPrice())
                      .totalPrice(itemTotalPrice)
                      .build();
            })
            .collect(Collectors.toList());

    items = itemRepository.saveAll(items);
    contract.setItems(items);

    // Create and add milestones if any
    if (request.getMilestones() != null && !request.getMilestones().isEmpty()) {
      List<ContractMilestone> milestones = request.getMilestones().stream()
              .map(milestoneRequest -> ContractMilestone.builder()
                      .contract(finalContract)
                      .title(milestoneRequest.getTitle())
                      .description(milestoneRequest.getDescription())
                      .dueDate(milestoneRequest.getDueDate())
                      .paymentAmount(milestoneRequest.getPaymentAmount())
                      .status(MilestoneStatus.PENDING)
                      .build())
              .collect(Collectors.toList());

      milestones = milestoneRepository.saveAll(milestones);
      contract.setMilestones(milestones);
    } else {
      contract.setMilestones(new ArrayList<>());
    }

    // Publish event for contract creation
    eventPublisher.publishContractCreatedEvent(contract);

    return contractMapper.toDto(contract);
  }

  @Override
  public ContractDTO getContractById(Long contractId) {
    log.info("Retrieving contract with ID: {}", contractId);

    Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new ContractNotFoundException("Contract not found with ID: " + contractId));

    // Fetch items and milestones
    List<ContractItem> items = itemRepository.findByContractId(contractId);
    List<ContractMilestone> milestones = milestoneRepository.findByContractId(contractId);

    contract.setItems(items);
    contract.setMilestones(milestones);

    return contractMapper.toDto(contract);
  }

  @Override
  public List<ContractDTO> getContractsByTenderId(Long tenderId) {
    log.info("Retrieving contracts for tender ID: {}", tenderId);

    List<Contract> contracts = contractRepository.findByTenderId(tenderId);
    return contractMapper.toDtoList(contracts);
  }

  @Override
  public Page<ContractDTO> getContractsByBidderId(Long bidderId, Pageable pageable) {
    log.info("Retrieving contracts for bidder ID: {}", bidderId);

    Page<Contract> contracts = contractRepository.findByBidderId(bidderId, pageable);
    return contracts.map(contractMapper::toDto);
  }

  @Override
  public Page<ContractDTO> searchContracts(String title, ContractStatus status, Long tenderId, Long bidderId, Pageable pageable) {
    log.info("Searching contracts with title: {}, status: {}, tenderId: {}, bidderId: {}",
            title, status, tenderId, bidderId);

    Page<Contract> contracts = contractRepository.searchContracts(title, status, tenderId, bidderId, pageable);
    return contracts.map(contractMapper::toDto);
  }

  @Override
  @Transactional
  public ContractDTO updateContractStatus(Long contractId, ContractStatus newStatus, String username) {
    log.info("Updating contract status to {} for contract ID: {}", newStatus, contractId);

    Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new ContractNotFoundException("Contract not found with ID: " + contractId));

    ContractStatus oldStatus = contract.getStatus();
    validateManualContractStatusTransition(oldStatus, newStatus);
    contract.setStatus(newStatus);
    contract.setUpdatedBy(username);

    contract = contractRepository.save(contract);

    // Publish event for status change
    eventPublisher.publishContractStatusChangedEvent(contract, oldStatus);

    return contractMapper.toDto(contract);
  }

  @Override
  @Transactional
  public ContractDTO activateContract(Long contractId, String username) {
    log.info("Activating contract with ID: {}", contractId);

    Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new ContractNotFoundException("Contract not found with ID: " + contractId));

    if (contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
      throw new IllegalStateException("Only contracts in PENDING_SIGNATURE status can be activated");
    }

    contract.setStatus(ContractStatus.ACTIVE);
    contract.setUpdatedBy(username);
    contract = contractRepository.save(contract);

    // Publish event for contract activation
    eventPublisher.publishContractActivatedEvent(contract);

    return contractMapper.toDto(contract);
  }

  @Override
  @Transactional
  public ContractDTO completeContract(Long contractId, String username) {
    log.info("Completing contract with ID: {}", contractId);

    Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new ContractNotFoundException("Contract not found with ID: " + contractId));

    if (contract.getStatus() != ContractStatus.ACTIVE) {
      throw new IllegalStateException("Only active contracts can be completed");
    }

    contract.setStatus(ContractStatus.COMPLETED);
    contract.setUpdatedBy(username);
    contract = contractRepository.save(contract);

    // Publish event for contract completion
    eventPublisher.publishContractCompletedEvent(contract);

    return contractMapper.toDto(contract);
  }

  @Override
  @Transactional
  public ContractDTO terminateContract(Long contractId, String reason, String username) {
    log.info("Terminating contract with ID: {}", contractId);

    Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new ContractNotFoundException("Contract not found with ID: " + contractId));

    if (contract.getStatus() != ContractStatus.ACTIVE) {
      throw new IllegalStateException("Only active contracts can be terminated");
    }

    contract.setStatus(ContractStatus.TERMINATED);
    contract.setDescription(contract.getDescription() + "\nTermination reason: " + reason);
    contract.setUpdatedBy(username);
    contract = contractRepository.save(contract);

    // Publish event for contract termination
    eventPublisher.publishContractTerminatedEvent(contract, reason);

    return contractMapper.toDto(contract);
  }

  @Override
  @Scheduled(cron = "0 0 * * * *") // Run every hour
  @Transactional
  public void checkForCompletedContracts() {
    log.info("Checking for completed contracts");

    LocalDate today = LocalDate.now();
    List<Contract> expiredContracts = contractRepository.findByStatusAndEndDateBefore(ContractStatus.ACTIVE, today);

    for (Contract contract : expiredContracts) {
      log.info("Auto-completing contract: {}", contract.getId());
      contract.setStatus(ContractStatus.COMPLETED);
      contract.setUpdatedBy("system");
      contract = contractRepository.save(contract);

      // Publish event for contract completion
      eventPublisher.publishContractCompletedEvent(contract);
    }

    log.info("Completed {} expired contracts", expiredContracts.size());
  }

  private void validateManualContractStatusTransition(ContractStatus currentStatus, ContractStatus newStatus) {
    if (currentStatus == newStatus) {
      return;
    }

    EnumSet<ContractStatus> allowedTargets = ALLOWED_MANUAL_STATUS_TRANSITIONS.get(currentStatus);
    if (allowedTargets == null || !allowedTargets.contains(newStatus)) {
      throw new IllegalStateException(
              "Manual contract status transition from " + currentStatus + " to " + newStatus + " is not allowed");
    }
  }

  private void validateAwardedBidEligibility(CreateContractRequest request) {
    if (contractRepository.existsByTenderIdAndBidderId(request.getTenderId(), request.getBidderId())) {
      throw new IllegalStateException("A contract already exists for this tender and bidder");
    }

    List<BidDTO> awardedBids;
    try {
      awardedBids = bidClient.getAwardedBidsByTender(request.getTenderId());
    } catch (FeignException ex) {
      throw new IllegalStateException(
              "Unable to validate awarded bid for tender " + request.getTenderId(), ex);
    }

    if (awardedBids == null || awardedBids.isEmpty()) {
      throw new IllegalStateException("Cannot create contract before a bidder is awarded for this tender");
    }

    List<BidDTO> matchingAwardedBids = awardedBids.stream()
            .filter(bid -> request.getBidderId().equals(bid.getTendererId()))
            .toList();

    if (matchingAwardedBids.isEmpty()) {
      throw new IllegalStateException("The requested bidder is not the awarded bidder for this tender");
    }

    if (matchingAwardedBids.size() > 1) {
      throw new IllegalStateException("Multiple awarded bids exist for this tender and bidder");
    }
  }
}
