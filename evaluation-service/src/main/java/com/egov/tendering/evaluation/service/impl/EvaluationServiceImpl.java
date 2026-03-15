package com.egov.tendering.evaluation.service.impl;

import com.egov.tendering.dto.BidDTO;
import com.egov.tendering.dto.TenderCriteriaDTO;
import com.egov.tendering.evaluation.client.BidClient;
import com.egov.tendering.evaluation.client.TenderClient;
import com.egov.tendering.dto.TenderDTO;
import com.egov.tendering.evaluation.dal.dto.*;
import com.egov.tendering.evaluation.dal.mapper.CriteriaScoreMapper;
import com.egov.tendering.evaluation.dal.mapper.EvaluationMapper;
import com.egov.tendering.evaluation.dal.model.CriteriaScore;
import com.egov.tendering.evaluation.dal.model.Evaluation;
import com.egov.tendering.evaluation.dal.model.EvaluationStatus;
import com.egov.tendering.evaluation.dal.repository.CriteriaScoreRepository;
import com.egov.tendering.evaluation.dal.repository.EvaluationRepository;
import com.egov.tendering.evaluation.event.EvaluationEventPublisher;
import com.egov.tendering.evaluation.exception.EvaluationNotFoundException;
import com.egov.tendering.evaluation.exception.InvalidEvaluationStateException;
import com.egov.tendering.evaluation.service.AllocationService;
import com.egov.tendering.evaluation.service.EvaluationService;
import com.egov.tendering.evaluation.service.TenderRankingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final CriteriaScoreRepository criteriaScoreRepository;
    private final EvaluationMapper evaluationMapper;
    private final CriteriaScoreMapper criteriaScoreMapper;
    private final EvaluationEventPublisher eventPublisher;
    private final TenderClient tenderClient;
    private final BidClient bidClient;
    private final TenderRankingService rankingService;
    private final AllocationService allocationService;

    @Override
    @Transactional
    public EvaluationDTO createEvaluation(Long tenderId, EvaluationRequest request, Long evaluatorId) {
        log.info("Creating evaluation for bid ID: {} in tender ID: {} by event ID: {}",
                request.getBidId(), tenderId, evaluatorId);

        // Check if evaluation already exists for this bid and event
        evaluationRepository.findByBidIdAndEvaluatorId(request.getBidId(), evaluatorId)
                .ifPresent(evaluation -> {
                    throw new IllegalStateException("Evaluation already exists for this bid and event");
                });

        // Create and save evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setTenderId(tenderId);
        evaluation.setBidId(request.getBidId());
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setStatus(EvaluationStatus.IN_PROGRESS);
        evaluation.setComments(request.getComments());

        evaluation = evaluationRepository.save(evaluation);

        // Create and save criteria scores
        List<CriteriaScore> criteriaScores = new ArrayList<>();
        BigDecimal totalWeightedScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        // Fetch tender details to get criteria weights
        TenderDTO tenderDTO = tenderClient.getTenderById(tenderId);
        Map<Long, BigDecimal> criteriaWeights = tenderDTO.getCriteria().stream()
                .collect(Collectors.toMap(TenderCriteriaDTO::getId, TenderCriteriaDTO::getWeight));

        for (CriteriaScoreRequest scoreRequest : request.getCriteriaScores()) {
            CriteriaScore criteriaScore = new CriteriaScore();
            criteriaScore.setEvaluation(evaluation);
            criteriaScore.setCriteriaId(scoreRequest.getCriteriaId());
            criteriaScore.setScore(scoreRequest.getScore());
            criteriaScore.setJustification(scoreRequest.getJustification());

            criteriaScores.add(criteriaScore);

            // Calculate weighted score
            BigDecimal weight = criteriaWeights.getOrDefault(
                    scoreRequest.getCriteriaId(), BigDecimal.ONE);
            totalWeightedScore = totalWeightedScore.add(
                    scoreRequest.getScore().multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        // Save criteria scores
        criteriaScores = criteriaScoreRepository.saveAll(criteriaScores);
        evaluation.setCriteriaScores(criteriaScores);

        // Calculate and set overall score
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal overallScore = totalWeightedScore.divide(totalWeight, 2, RoundingMode.HALF_UP);
            evaluation.setOverallScore(overallScore);
            evaluation = evaluationRepository.save(evaluation);
        }

        // Publish event
        eventPublisher.publishEvaluationCreatedEvent(evaluation);

        return enrichEvaluationDTO(evaluationMapper.toDto(evaluation));
    }

    @Override
    public EvaluationDTO getEvaluationById(Long evaluationId) {
        log.info("Getting evaluation by ID: {}", evaluationId);

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new EvaluationNotFoundException(evaluationId));

        return enrichEvaluationDTO(evaluationMapper.toDto(evaluation));
    }

    @Override
    @Transactional
    public EvaluationDTO updateEvaluation(Long evaluationId, EvaluationRequest request) {
        log.info("Updating evaluation ID: {} for bid ID: {}", evaluationId, request.getBidId());

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new EvaluationNotFoundException(evaluationId));

        if (evaluation.getStatus() == EvaluationStatus.COMPLETED) {
            throw new InvalidEvaluationStateException("Cannot update a completed evaluation");
        }

        // Update evaluation
        evaluation.setComments(request.getComments());
        evaluation.setStatus(EvaluationStatus.IN_PROGRESS);

        // Clear existing criteria scores
        evaluation.getCriteriaScores().clear();
        criteriaScoreRepository.deleteAll(evaluation.getCriteriaScores());

        // Create and save new criteria scores
        List<CriteriaScore> criteriaScores = new ArrayList<>();
        BigDecimal totalWeightedScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        // Fetch tender details to get criteria weights
        TenderDTO tenderDTO = tenderClient.getTenderById(evaluation.getTenderId());
        Map<Long, BigDecimal> criteriaWeights = tenderDTO.getCriteria().stream()
                .collect(Collectors.toMap(TenderCriteriaDTO::getId, TenderCriteriaDTO::getWeight));

        for (CriteriaScoreRequest scoreRequest : request.getCriteriaScores()) {
            CriteriaScore criteriaScore = new CriteriaScore();
            criteriaScore.setEvaluation(evaluation);
            criteriaScore.setCriteriaId(scoreRequest.getCriteriaId());
            criteriaScore.setScore(scoreRequest.getScore());
            criteriaScore.setJustification(scoreRequest.getJustification());

            criteriaScores.add(criteriaScore);

            // Calculate weighted score
            BigDecimal weight = criteriaWeights.getOrDefault(
                    scoreRequest.getCriteriaId(), BigDecimal.ONE);
            totalWeightedScore = totalWeightedScore.add(
                    scoreRequest.getScore().multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        // Save criteria scores
        criteriaScores = criteriaScoreRepository.saveAll(criteriaScores);
        evaluation.setCriteriaScores(criteriaScores);

        // Calculate and set overall score
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal overallScore = totalWeightedScore.divide(totalWeight, 2, RoundingMode.HALF_UP);
            evaluation.setOverallScore(overallScore);
        }

        evaluation = evaluationRepository.save(evaluation);

        // Publish event
        eventPublisher.publishEvaluationUpdatedEvent(evaluation);

        return enrichEvaluationDTO(evaluationMapper.toDto(evaluation));
    }

    @Override
    @Transactional
    public EvaluationDTO updateEvaluationStatus(Long evaluationId, EvaluationStatus status) {
        log.info("Updating evaluation ID: {} status to: {}", evaluationId, status);

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new EvaluationNotFoundException(evaluationId));

        EvaluationStatus oldStatus = evaluation.getStatus();
        evaluation.setStatus(status);
        evaluation = evaluationRepository.save(evaluation);

        // Publish event
        eventPublisher.publishEvaluationStatusChangedEvent(evaluation, oldStatus);

        return enrichEvaluationDTO(evaluationMapper.toDto(evaluation));
    }

    @Override
    @Transactional
    public void deleteEvaluation(Long evaluationId) {
        log.info("Deleting evaluation ID: {}", evaluationId);

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new EvaluationNotFoundException(evaluationId));

        // Only allow deletion of evaluations in PENDING or IN_PROGRESS status
        if (evaluation.getStatus() == EvaluationStatus.COMPLETED) {
            throw new InvalidEvaluationStateException("Cannot delete a completed evaluation");
        }

        // Publish event before deletion
        eventPublisher.publishEvaluationDeletedEvent(evaluation);

        evaluationRepository.delete(evaluation);
    }

    @Override
    public List<EvaluationDTO> getEvaluationsByTender(Long tenderId) {
        log.info("Getting evaluations for tender ID: {}", tenderId);

        List<Evaluation> evaluations = evaluationRepository.findByTenderId(tenderId);

        return enrichEvaluationDTOs(evaluationMapper.toDtoList(evaluations));
    }

    @Override
    public List<EvaluationDTO> getEvaluationsByBid(Long bidId) {
        log.info("Getting evaluations for bid ID: {}", bidId);

        List<Evaluation> evaluations = evaluationRepository.findByBidId(bidId);

        return enrichEvaluationDTOs(evaluationMapper.toDtoList(evaluations));
    }

    @Override
    public Page<EvaluationDTO> getEvaluationsByEvaluator(Long evaluatorId, Pageable pageable) {
        log.info("Getting evaluations for event ID: {}", evaluatorId);

        Page<Evaluation> evaluations = evaluationRepository.findAll(pageable);

        return evaluations.map(evaluation -> enrichEvaluationDTO(evaluationMapper.toDto(evaluation)));
    }

    @Override
    public EvaluationDTO getEvaluationByBidAndEvaluator(Long bidId, Long evaluatorId) {
        log.info("Getting evaluation for bid ID: {} and event ID: {}", bidId, evaluatorId);

        Evaluation evaluation = evaluationRepository.findByBidIdAndEvaluatorId(bidId, evaluatorId)
                .orElseThrow(() -> new EvaluationNotFoundException("Evaluation not found for bid: " + bidId +
                        " and event: " + evaluatorId));

        return enrichEvaluationDTO(evaluationMapper.toDto(evaluation));
    }

    @Override
    @Transactional
    public TenderEvaluationResult calculateTenderResults(Long tenderId) {
        log.info("Calculating tender results for tender ID: {}", tenderId);

        // Fetch all completed evaluations for this tender
        List<Evaluation> evaluations = evaluationRepository.findByTenderIdAndStatus(
                tenderId, EvaluationStatus.COMPLETED);

        if (evaluations.isEmpty()) {
            throw new IllegalStateException("No completed evaluations found for tender ID: " + tenderId);
        }

        // Get tender details including allocation strategy
        TenderDTO tenderDTO = tenderClient.getTenderById(tenderId);

        // Calculate bid rankings
        List<TenderRankingDTO> rankings = rankingService.calculateBidRankings(tenderId, evaluations);

        // Determine winners and allocate items based on allocation strategy
        List<AllocationResultDTO> allocations;

        switch (tenderDTO.getAllocationStrategy()) {
            case SINGLE:
                allocations = allocationService.allocateSingleWinner(tenderId, rankings);
                break;
            case COOPERATIVE:
                allocations = allocationService.allocateCooperative(tenderId, evaluations);
                break;
            case COMPETITIVE:
                BigDecimal cutoffScore = tenderDTO.getCutoffScore();
                boolean isAverageAllocation = tenderDTO.getIsAverageAllocation();
                allocations = allocationService.allocateCompetitive(
                        tenderId, rankings, cutoffScore, isAverageAllocation);
                break;
            default:
                throw new IllegalArgumentException("Unsupported allocation strategy: " +
                        tenderDTO.getAllocationStrategy());
        }

        // Publish event for completed evaluation
        eventPublisher.publishTenderEvaluationCompletedEvent(
                tenderId, tenderDTO.getTitle(), rankings, allocations);

        // Return evaluation result
        return TenderEvaluationResult.builder()
                .tenderId(tenderId)
                .rankings(rankings)
                .allocations(allocations)
                .completed(true)
                .build();
    }

    // Helper method to enrich evaluation DTO with bidder name
    private EvaluationDTO enrichEvaluationDTO(EvaluationDTO evaluationDTO) {
        try {
            BidDTO bidDTO = bidClient.getBidById(evaluationDTO.getBidId());
            evaluationDTO.setBidderName(bidDTO.getTendererName());

            // Enrich criteria scores with criteria names
            TenderDTO tenderDTO = tenderClient.getTenderById(evaluationDTO.getTenderId());
            Map<Long, String> criteriaNames = tenderDTO.getCriteria().stream()
                    .collect(Collectors.toMap(TenderCriteriaDTO::getId, TenderCriteriaDTO::getName));

            if (evaluationDTO.getCriteriaScores() != null) {
                evaluationDTO.getCriteriaScores().forEach(cs ->
                        cs.setCriteriaName(criteriaNames.getOrDefault(cs.getCriteriaId(), "Unknown")));
            }
        } catch (Exception e) {
            log.warn("Failed to enrich evaluation DTO: {}", e.getMessage());
        }
        return evaluationDTO;
    }

    // Helper method to enrich a list of evaluation DTOs
    private List<EvaluationDTO> enrichEvaluationDTOs(List<EvaluationDTO> evaluationDTOs) {
        return evaluationDTOs.stream()
                .map(this::enrichEvaluationDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets all evaluations for a tender as entities (not DTOs)
     * This is used by the allocation service
     */
    @Override
    public List<Evaluation> getEvaluationsByTenderId(Long tenderId) {
        log.info("Getting evaluation entities for tender ID: {}", tenderId);

        // Only include completed evaluations for allocation
        return evaluationRepository.findByTenderIdAndStatus(tenderId, EvaluationStatus.COMPLETED);
    }
}
