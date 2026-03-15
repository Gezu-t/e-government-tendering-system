package com.egov.tendering.evaluation.service.impl;

import com.egov.tendering.dto.BidDTO;
import com.egov.tendering.dto.TenderCriteriaDTO;
import com.egov.tendering.dto.TenderDTO;
import com.egov.tendering.evaluation.client.BidClient;
import com.egov.tendering.evaluation.client.TenderClient;
import com.egov.tendering.evaluation.dal.dto.EvaluationCategoryConfigDTO;
import com.egov.tendering.evaluation.dal.dto.EvaluationScoreSummaryDTO;
import com.egov.tendering.evaluation.dal.dto.MultiCriteriaEvaluationResult;
import com.egov.tendering.evaluation.dal.model.*;
import com.egov.tendering.evaluation.dal.repository.EvaluationCategoryConfigRepository;
import com.egov.tendering.evaluation.dal.repository.EvaluationRepository;
import com.egov.tendering.evaluation.dal.repository.EvaluationScoreSummaryRepository;
import com.egov.tendering.evaluation.exception.EvaluationNotFoundException;
import com.egov.tendering.evaluation.service.MultiCriteriaEvaluationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiCriteriaEvaluationServiceImpl implements MultiCriteriaEvaluationService {

    private final EvaluationCategoryConfigRepository categoryConfigRepository;
    private final EvaluationScoreSummaryRepository scoreSummaryRepository;
    private final EvaluationRepository evaluationRepository;
    private final TenderClient tenderClient;
    private final BidClient bidClient;

    @Override
    @Transactional
    public List<EvaluationCategoryConfigDTO> configureCategories(Long tenderId,
                                                                  List<EvaluationCategoryConfigDTO> configs) {
        log.info("Configuring {} evaluation categories for tender: {}", configs.size(), tenderId);

        // Validate total weight = 100
        BigDecimal totalWeight = configs.stream()
                .map(EvaluationCategoryConfigDTO::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeight.compareTo(new BigDecimal("100.0")) != 0) {
            throw new IllegalArgumentException("Category weights must sum to 100. Current total: " + totalWeight);
        }

        // Clear existing configs
        categoryConfigRepository.deleteByTenderId(tenderId);

        List<EvaluationCategoryConfig> savedConfigs = configs.stream()
                .map(dto -> {
                    EvaluationCategoryConfig config = EvaluationCategoryConfig.builder()
                            .tenderId(tenderId)
                            .category(dto.getCategory())
                            .weight(dto.getWeight())
                            .passThreshold(dto.getPassThreshold())
                            .mandatory(dto.getMandatory())
                            .description(dto.getDescription())
                            .build();
                    return categoryConfigRepository.save(config);
                })
                .toList();

        log.info("Configured {} categories for tender {}", savedConfigs.size(), tenderId);

        return savedConfigs.stream().map(this::toConfigDTO).collect(Collectors.toList());
    }

    @Override
    public List<EvaluationCategoryConfigDTO> getCategoryConfigs(Long tenderId) {
        return categoryConfigRepository.findByTenderId(tenderId)
                .stream()
                .map(this::toConfigDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<EvaluationScoreSummaryDTO> computeScoreBreakdown(Long evaluationId) {
        log.info("Computing score breakdown for evaluation: {}", evaluationId);

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new EvaluationNotFoundException(evaluationId));

        // Get category configs for this tender
        List<EvaluationCategoryConfig> configs = categoryConfigRepository
                .findByTenderId(evaluation.getTenderId());

        if (configs.isEmpty()) {
            log.info("No category configs found for tender {}. Using default single category.", evaluation.getTenderId());
            return List.of();
        }

        // Get tender criteria to map criteria -> category
        TenderDTO tenderDTO = tenderClient.getTenderById(evaluation.getTenderId());
        Map<Long, String> criteriaTypeMap = tenderDTO.getCriteria().stream()
                .collect(Collectors.toMap(TenderCriteriaDTO::getId, TenderCriteriaDTO::getCriteriaType));

        // Map criteria types to score categories
        Map<ScoreCategory, List<CriteriaScore>> scoresByCategory = new HashMap<>();
        for (CriteriaScore cs : evaluation.getCriteriaScores()) {
            String criteriaType = criteriaTypeMap.getOrDefault(cs.getCriteriaId(), "QUALITY");
            ScoreCategory category = mapCriteriaTypeToCategory(criteriaType);
            scoresByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(cs);
        }

        // Clear existing summaries
        scoreSummaryRepository.deleteByEvaluationId(evaluationId);

        // Compute summary per category
        List<EvaluationScoreSummary> summaries = new ArrayList<>();
        for (EvaluationCategoryConfig config : configs) {
            List<CriteriaScore> categoryScores = scoresByCategory.getOrDefault(config.getCategory(), List.of());

            BigDecimal rawScore = BigDecimal.ZERO;
            BigDecimal maxPossible = BigDecimal.valueOf(categoryScores.size() * 10L); // Max score per criteria = 10

            if (!categoryScores.isEmpty()) {
                rawScore = categoryScores.stream()
                        .map(CriteriaScore::getScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(categoryScores.size()), 2, RoundingMode.HALF_UP);
            }

            BigDecimal weightedScore = rawScore.multiply(config.getWeight())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            boolean passed = config.getPassThreshold() == null
                    || rawScore.compareTo(config.getPassThreshold()) >= 0;

            EvaluationScoreSummary summary = EvaluationScoreSummary.builder()
                    .evaluationId(evaluationId)
                    .category(config.getCategory())
                    .categoryWeight(config.getWeight())
                    .rawScore(rawScore)
                    .weightedScore(weightedScore)
                    .maxPossibleScore(maxPossible)
                    .criteriaCount(categoryScores.size())
                    .passThreshold(config.getPassThreshold())
                    .passed(passed)
                    .build();

            summaries.add(scoreSummaryRepository.save(summary));
        }

        log.info("Computed {} category scores for evaluation {}", summaries.size(), evaluationId);
        return summaries.stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    @Override
    public MultiCriteriaEvaluationResult getMultiCriteriaResult(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new EvaluationNotFoundException(evaluationId));

        List<EvaluationScoreSummary> summaries = scoreSummaryRepository.findByEvaluationId(evaluationId);

        // If no summaries computed yet, compute them
        List<EvaluationScoreSummaryDTO> breakdown;
        if (summaries.isEmpty()) {
            breakdown = computeScoreBreakdown(evaluationId);
        } else {
            breakdown = summaries.stream().map(this::toSummaryDTO).collect(Collectors.toList());
        }

        // Get category configs to check mandatory categories
        List<EvaluationCategoryConfig> configs = categoryConfigRepository
                .findByTenderId(evaluation.getTenderId());

        Set<ScoreCategory> mandatoryCategories = configs.stream()
                .filter(c -> Boolean.TRUE.equals(c.getMandatory()))
                .map(EvaluationCategoryConfig::getCategory)
                .collect(Collectors.toSet());

        boolean allMandatoryPassed = breakdown.stream()
                .filter(b -> mandatoryCategories.contains(b.getCategory()))
                .allMatch(b -> Boolean.TRUE.equals(b.getPassed()));

        boolean allPassed = breakdown.stream()
                .allMatch(b -> Boolean.TRUE.equals(b.getPassed()));

        // Get bidder name
        String bidderName = "Unknown";
        try {
            BidDTO bidDTO = bidClient.getBidById(evaluation.getBidId());
            bidderName = bidDTO.getTendererName();
        } catch (Exception e) {
            log.warn("Failed to get bidder name for bid: {}", evaluation.getBidId());
        }

        return MultiCriteriaEvaluationResult.builder()
                .evaluationId(evaluationId)
                .bidId(evaluation.getBidId())
                .bidderName(bidderName)
                .overallScore(evaluation.getOverallScore())
                .categoryBreakdown(breakdown)
                .allMandatoryCategoriesPassed(allMandatoryPassed)
                .qualified(allMandatoryPassed && allPassed)
                .build();
    }

    @Override
    public List<MultiCriteriaEvaluationResult> getMultiCriteriaResultsForTender(Long tenderId) {
        log.info("Getting multi-criteria results for tender: {}", tenderId);

        List<Evaluation> evaluations = evaluationRepository.findByTenderId(tenderId);

        return evaluations.stream()
                .map(e -> getMultiCriteriaResult(e.getId()))
                .sorted(Comparator.comparing(MultiCriteriaEvaluationResult::getOverallScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private ScoreCategory mapCriteriaTypeToCategory(String criteriaType) {
        return switch (criteriaType.toUpperCase()) {
            case "PRICE" -> ScoreCategory.FINANCIAL;
            case "QUALITY" -> ScoreCategory.QUALITY;
            case "EXPERIENCE" -> ScoreCategory.EXPERIENCE;
            case "TIME", "QUANTITY", "ENUMERATION" -> ScoreCategory.TECHNICAL;
            default -> ScoreCategory.TECHNICAL;
        };
    }

    private EvaluationCategoryConfigDTO toConfigDTO(EvaluationCategoryConfig config) {
        return EvaluationCategoryConfigDTO.builder()
                .id(config.getId())
                .category(config.getCategory())
                .weight(config.getWeight())
                .passThreshold(config.getPassThreshold())
                .mandatory(config.getMandatory())
                .description(config.getDescription())
                .build();
    }

    private EvaluationScoreSummaryDTO toSummaryDTO(EvaluationScoreSummary summary) {
        return EvaluationScoreSummaryDTO.builder()
                .id(summary.getId())
                .evaluationId(summary.getEvaluationId())
                .category(summary.getCategory())
                .categoryWeight(summary.getCategoryWeight())
                .rawScore(summary.getRawScore())
                .weightedScore(summary.getWeightedScore())
                .maxPossibleScore(summary.getMaxPossibleScore())
                .criteriaCount(summary.getCriteriaCount())
                .passThreshold(summary.getPassThreshold())
                .passed(summary.getPassed())
                .build();
    }
}
