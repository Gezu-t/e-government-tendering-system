package com.egov.tendering.tender.service.impl;


import com.egov.tendering.tender.dal.dto.*;
import com.egov.tendering.tender.dal.mapper.TenderMapper;
import com.egov.tendering.tender.dal.model.*;
import com.egov.tendering.tender.dal.repository.TenderAmendmentRepository;
import com.egov.tendering.tender.dal.repository.TenderCriteriaRepository;
import com.egov.tendering.tender.dal.repository.TenderItemRepository;
import com.egov.tendering.tender.dal.repository.TenderRepository;
import com.egov.tendering.tender.event.TenderEventPublisher;
import com.egov.tendering.tender.exception.TenderNotFoundException;
import com.egov.tendering.tender.service.TenderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenderServiceImpl implements TenderService {

  private static final Map<TenderStatus, EnumSet<TenderStatus>> ALLOWED_MANUAL_STATUS_TRANSITIONS = Map.of(
          TenderStatus.DRAFT, EnumSet.of(TenderStatus.CANCELLED),
          TenderStatus.PUBLISHED, EnumSet.of(TenderStatus.AMENDED, TenderStatus.CANCELLED),
          TenderStatus.AMENDED, EnumSet.of(TenderStatus.CANCELLED),
          TenderStatus.CLOSED, EnumSet.of(TenderStatus.EVALUATION_IN_PROGRESS, TenderStatus.CANCELLED),
          TenderStatus.EVALUATION_IN_PROGRESS, EnumSet.of(TenderStatus.EVALUATED, TenderStatus.CANCELLED),
          TenderStatus.EVALUATED, EnumSet.of(TenderStatus.AWARDED, TenderStatus.CANCELLED)
  );
  private static final List<TenderStatus> PUBLICLY_VISIBLE_STATUSES = List.of(
          TenderStatus.PUBLISHED,
          TenderStatus.AMENDED,
          TenderStatus.CLOSED,
          TenderStatus.AWARDED,
          TenderStatus.CANCELLED
  );

  private final TenderRepository tenderRepository;
  private final TenderCriteriaRepository criteriaRepository;
  private final TenderItemRepository itemRepository;
  private final TenderAmendmentRepository amendmentRepository;
  private final TenderMapper tenderMapper;
  private final TenderEventPublisher eventPublisher;

  @Override
  @Transactional
  public TenderDTO createTender(CreateTenderRequest request, Long tendereeId) {
    log.info("Creating new tender: {} by tenderee: {}", request.getTitle(), tendereeId);

    Tender tender = Tender.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .tendereeId(tendereeId)
            .type(request.getType())
            .status(TenderStatus.DRAFT)
            .submissionDeadline(request.getSubmissionDeadline())
            .allocationStrategy(request.getAllocationStrategy())
            .minWinners(request.getMinWinners())
            .maxWinners(request.getMaxWinners())
            .cutoffScore(request.getCutoffScore())
            .isAverageAllocation(request.getIsAverageAllocation())
            .build();

    // Save tender first to get ID
    tender = tenderRepository.save(tender);

    // Create and add criteria
    Tender finalTender = tender;
    List<TenderCriteria> criteriaList = request.getCriteria().stream()
            .map(criteriaRequest -> TenderCriteria.builder()
                    .tender(finalTender)
                    .name(criteriaRequest.getName())
                    .description(criteriaRequest.getDescription())
                    .type(criteriaRequest.getType())
                    .weight(criteriaRequest.getWeight())
                    .preferHigher(criteriaRequest.getPreferHigher())
                    .build())
            .collect(Collectors.toList());

    criteriaList = criteriaRepository.saveAll(criteriaList);
    tender.setCriteria(criteriaList);

    // Create and add items
    List<TenderItem> itemList = new ArrayList<>();
    final List<TenderCriteria> finalCriteriaList = criteriaList; // Create a final copy
    for (var itemRequest : request.getItems()) {
      TenderCriteria criteria = finalCriteriaList.stream()
              .filter(c -> c.getId().equals(itemRequest.getCriteriaId()))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Invalid criteria ID: " + itemRequest.getCriteriaId()));

      TenderItem item = TenderItem.builder()
              .tender(tender)
              .criteria(criteria)
              .name(itemRequest.getName())
              .description(itemRequest.getDescription())
              .quantity(itemRequest.getQuantity())
              .unit(itemRequest.getUnit())
              .estimatedPrice(itemRequest.getEstimatedPrice())
              .build();

      itemList.add(item);
    }

    itemList = itemRepository.saveAll(itemList);
    tender.setItems(itemList);

    // Publish event for tender creation
    eventPublisher.publishTenderCreatedEvent(tender);

    return tenderMapper.toDto(tender);
  }

  @Override
  public TenderDTO getTenderById(Long tenderId) {
    log.info("Retrieving tender with ID: {}", tenderId);

    Tender tender = tenderRepository.findById(tenderId)
            .orElseThrow(() -> new TenderNotFoundException("Tender not found with ID: " + tenderId));
    return tenderMapper.toDto(tender);
  }

  @Override
  public Page<TenderDTO> searchTenders(String title, TenderStatus status, TenderType type, Pageable pageable) {
    log.info("Searching tenders with title: {}, status: {}, type: {}", title, status, type);

    if (status != null && !PUBLICLY_VISIBLE_STATUSES.contains(status)) {
      throw new IllegalArgumentException("Only publicly visible tender statuses can be searched from the public catalogue");
    }

    Page<Tender> tenders = status != null
            ? tenderRepository.searchPublicTenders(title, List.of(status), type, pageable)
            : tenderRepository.searchPublicTenders(title, PUBLICLY_VISIBLE_STATUSES, type, pageable);
    return tenders.map(tenderMapper::toDto);
  }

  @Override
  public Page<TenderDTO> getTendersByTenderee(Long tendereeId, Pageable pageable) {
    log.info("Retrieving tenders by tenderee ID: {}", tendereeId);

    Page<Tender> tenders = tenderRepository.findByTendereeId(tendereeId, pageable);
    return tenders.map(tenderMapper::toDto);
  }

  @Override
  @Transactional
  public TenderDTO updateTenderStatus(Long tenderId, UpdateTenderStatusRequest request) {
    log.info("Updating tender status: {} for tender ID: {}", request.getStatus(), tenderId);

    Tender tender = tenderRepository.findById(tenderId)
            .orElseThrow(() -> new TenderNotFoundException("Tender not found with ID: " + tenderId));

    TenderStatus oldStatus = tender.getStatus();
    validateManualTenderStatusTransition(oldStatus, request.getStatus());
    tender.setStatus(request.getStatus());
    tender = tenderRepository.save(tender);

    // Publish event for status change
    eventPublisher.publishTenderStatusChangedEvent(tender, oldStatus);

    return tenderMapper.toDto(tender);
  }

  @Override
  @Transactional
  public TenderDTO publishTender(Long tenderId) {
    log.info("Publishing tender with ID: {}", tenderId);

    Tender tender = tenderRepository.findById(tenderId)
            .orElseThrow(() -> new TenderNotFoundException("Tender not found with ID: " + tenderId));

    if (tender.getStatus() != TenderStatus.DRAFT) {
      throw new IllegalStateException("Only tenders in DRAFT status can be published");
    }

    tender.setStatus(TenderStatus.PUBLISHED);
    tender = tenderRepository.save(tender);

    // Publish event for tender publishing
    eventPublisher.publishTenderPublishedEvent(tender);

    return tenderMapper.toDto(tender);
  }

  @Override
  @Transactional
  public TenderDTO closeTender(Long tenderId) {
    log.info("Closing tender with ID: {}", tenderId);

    Tender tender = tenderRepository.findById(tenderId)
            .orElseThrow(() -> new TenderNotFoundException("Tender not found with ID: " + tenderId));

    if (tender.getStatus() != TenderStatus.PUBLISHED) {
      throw new IllegalStateException("Only published tenders can be closed");
    }

    tender.setStatus(TenderStatus.CLOSED);
    tender = tenderRepository.save(tender);

    // Publish event for tender closing
    eventPublisher.publishTenderClosedEvent(tender);

    return tenderMapper.toDto(tender);
  }

  @Override
  @Transactional
  public TenderDTO amendTender(Long tenderId, TenderAmendmentRequest request, Long amendedBy) {
    log.info("Amending tender: {} by user: {}", tenderId, amendedBy);

    Tender tender = tenderRepository.findById(tenderId)
            .orElseThrow(() -> new TenderNotFoundException("Tender not found with ID: " + tenderId));

    if (tender.getStatus() != TenderStatus.PUBLISHED && tender.getStatus() != TenderStatus.AMENDED) {
      throw new IllegalStateException("Only PUBLISHED or AMENDED tenders can be amended");
    }

    // Record the amendment
    Integer nextAmendmentNumber = amendmentRepository.findMaxAmendmentNumber(tenderId) + 1;

    TenderAmendment amendment = TenderAmendment.builder()
            .tenderId(tenderId)
            .amendmentNumber(nextAmendmentNumber)
            .reason(request.getReason())
            .description(request.getDescription())
            .previousDeadline(tender.getSubmissionDeadline())
            .previousDescription(tender.getDescription())
            .amendedBy(amendedBy)
            .build();

    // Apply changes
    if (request.getDescription() != null) {
      tender.setDescription(request.getDescription());
    }
    if (request.getNewSubmissionDeadline() != null) {
      amendment.setNewDeadline(request.getNewSubmissionDeadline());
      tender.setSubmissionDeadline(request.getNewSubmissionDeadline());
    }

    tender.setStatus(TenderStatus.AMENDED);
    tender = tenderRepository.save(tender);
    amendmentRepository.save(amendment);

    // Publish amendment event to notify all bidders
    eventPublisher.publishTenderAmendedEvent(tender, amendment);

    log.info("Tender {} amended successfully. Amendment #{}", tenderId, nextAmendmentNumber);
    return tenderMapper.toDto(tender);
  }

  @Override
  public List<TenderAmendmentDTO> getTenderAmendments(Long tenderId) {
    log.info("Retrieving amendments for tender: {}", tenderId);

    return amendmentRepository.findByTenderIdOrderByAmendmentNumberDesc(tenderId)
            .stream()
            .map(a -> TenderAmendmentDTO.builder()
                    .id(a.getId())
                    .tenderId(a.getTenderId())
                    .amendmentNumber(a.getAmendmentNumber())
                    .reason(a.getReason())
                    .description(a.getDescription())
                    .previousDeadline(a.getPreviousDeadline())
                    .newDeadline(a.getNewDeadline())
                    .amendedBy(a.getAmendedBy())
                    .createdAt(a.getCreatedAt())
                    .build())
            .collect(Collectors.toList());
  }

  @Override
  @Scheduled(cron = "0 0 * * * *") // Run every hour
  @Transactional
  public void checkForExpiredTenders() {
    log.info("Checking for expired tenders");

    LocalDateTime now = LocalDateTime.now();
    List<Tender> expiredTenders = tenderRepository.findExpiredTenders(TenderStatus.PUBLISHED, now);

    for (Tender tender : expiredTenders) {
      log.info("Automatically closing expired tender: {}", tender.getId());
      tender.setStatus(TenderStatus.CLOSED);
      tender = tenderRepository.save(tender);

      // Publish event for tender closing
      eventPublisher.publishTenderClosedEvent(tender);
    }

    log.info("Closed {} expired tenders", expiredTenders.size());
  }

  private void validateManualTenderStatusTransition(TenderStatus currentStatus, TenderStatus newStatus) {
    if (currentStatus == newStatus) {
      return;
    }

    EnumSet<TenderStatus> allowedTargets = ALLOWED_MANUAL_STATUS_TRANSITIONS.get(currentStatus);
    if (allowedTargets == null || !allowedTargets.contains(newStatus)) {
      throw new IllegalStateException(
              "Manual tender status transition from " + currentStatus + " to " + newStatus + " is not allowed");
    }
  }
}
