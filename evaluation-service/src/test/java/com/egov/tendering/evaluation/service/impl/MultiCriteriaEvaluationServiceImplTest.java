package com.egov.tendering.evaluation.service.impl;

import com.egov.tendering.dto.BidDTO;
import com.egov.tendering.evaluation.client.BidClient;
import com.egov.tendering.evaluation.client.TenderClient;
import com.egov.tendering.evaluation.dal.dto.EvaluationCategoryConfigDTO;
import com.egov.tendering.evaluation.dal.dto.MultiCriteriaEvaluationResult;
import com.egov.tendering.evaluation.dal.model.*;
import com.egov.tendering.evaluation.dal.repository.EvaluationCategoryConfigRepository;
import com.egov.tendering.evaluation.dal.repository.EvaluationRepository;
import com.egov.tendering.evaluation.dal.repository.EvaluationScoreSummaryRepository;
import com.egov.tendering.evaluation.exception.EvaluationNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiCriteriaEvaluationServiceImplTest {

    @Mock
    private EvaluationCategoryConfigRepository categoryConfigRepository;

    @Mock
    private EvaluationScoreSummaryRepository scoreSummaryRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private TenderClient tenderClient;

    @Mock
    private BidClient bidClient;

    @InjectMocks
    private MultiCriteriaEvaluationServiceImpl multiCriteriaEvaluationService;

    private static final Long TENDER_ID = 1L;
    private static final Long EVALUATION_ID = 50L;
    private static final Long BID_ID = 10L;

    @Nested
    @DisplayName("configureCategories")
    class ConfigureCategories {

        @Test
        @DisplayName("should configure categories successfully when weights sum to 100")
        void shouldConfigureCategoriesSuccessfully() {
            // Arrange
            EvaluationCategoryConfigDTO technicalConfig = EvaluationCategoryConfigDTO.builder()
                    .category(ScoreCategory.TECHNICAL)
                    .weight(new BigDecimal("40.0"))
                    .passThreshold(new BigDecimal("5.0"))
                    .mandatory(true)
                    .description("Technical evaluation")
                    .build();

            EvaluationCategoryConfigDTO financialConfig = EvaluationCategoryConfigDTO.builder()
                    .category(ScoreCategory.FINANCIAL)
                    .weight(new BigDecimal("35.0"))
                    .passThreshold(new BigDecimal("4.0"))
                    .mandatory(true)
                    .description("Financial evaluation")
                    .build();

            EvaluationCategoryConfigDTO qualityConfig = EvaluationCategoryConfigDTO.builder()
                    .category(ScoreCategory.QUALITY)
                    .weight(new BigDecimal("25.0"))
                    .passThreshold(null)
                    .mandatory(false)
                    .description("Quality evaluation")
                    .build();

            List<EvaluationCategoryConfigDTO> configs = List.of(technicalConfig, financialConfig, qualityConfig);

            when(categoryConfigRepository.save(any(EvaluationCategoryConfig.class)))
                    .thenAnswer(invocation -> {
                        EvaluationCategoryConfig saved = invocation.getArgument(0);
                        saved.setId(1L);
                        return saved;
                    });

            // Act
            List<EvaluationCategoryConfigDTO> result =
                    multiCriteriaEvaluationService.configureCategories(TENDER_ID, configs);

            // Assert
            assertThat(result).hasSize(3);
            verify(categoryConfigRepository).deleteByTenderId(TENDER_ID);
            verify(categoryConfigRepository, times(3)).save(any(EvaluationCategoryConfig.class));
        }

        @Test
        @DisplayName("should throw when category weights do not sum to 100")
        void shouldThrowWhenWeightsDoNotSumTo100() {
            // Arrange
            EvaluationCategoryConfigDTO config1 = EvaluationCategoryConfigDTO.builder()
                    .category(ScoreCategory.TECHNICAL)
                    .weight(new BigDecimal("40.0"))
                    .mandatory(true)
                    .build();

            EvaluationCategoryConfigDTO config2 = EvaluationCategoryConfigDTO.builder()
                    .category(ScoreCategory.FINANCIAL)
                    .weight(new BigDecimal("30.0"))
                    .mandatory(true)
                    .build();

            // Total = 70, not 100
            List<EvaluationCategoryConfigDTO> configs = List.of(config1, config2);

            // Act & Assert
            assertThatThrownBy(() ->
                    multiCriteriaEvaluationService.configureCategories(TENDER_ID, configs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Category weights must sum to 100");

            verify(categoryConfigRepository, never()).deleteByTenderId(anyLong());
            verify(categoryConfigRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when weights exceed 100")
        void shouldThrowWhenWeightsExceed100() {
            // Arrange
            EvaluationCategoryConfigDTO config1 = EvaluationCategoryConfigDTO.builder()
                    .category(ScoreCategory.TECHNICAL)
                    .weight(new BigDecimal("60.0"))
                    .build();

            EvaluationCategoryConfigDTO config2 = EvaluationCategoryConfigDTO.builder()
                    .category(ScoreCategory.FINANCIAL)
                    .weight(new BigDecimal("50.0"))
                    .build();

            // Total = 110, not 100
            List<EvaluationCategoryConfigDTO> configs = List.of(config1, config2);

            // Act & Assert
            assertThatThrownBy(() ->
                    multiCriteriaEvaluationService.configureCategories(TENDER_ID, configs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Category weights must sum to 100");
        }
    }

    @Nested
    @DisplayName("getCategoryConfigs")
    class GetCategoryConfigs {

        @Test
        @DisplayName("should return category configs for a tender")
        void shouldReturnCategoryConfigsForTender() {
            // Arrange
            EvaluationCategoryConfig config1 = EvaluationCategoryConfig.builder()
                    .id(1L)
                    .tenderId(TENDER_ID)
                    .category(ScoreCategory.TECHNICAL)
                    .weight(new BigDecimal("60.0"))
                    .passThreshold(new BigDecimal("5.0"))
                    .mandatory(true)
                    .description("Technical evaluation")
                    .build();

            EvaluationCategoryConfig config2 = EvaluationCategoryConfig.builder()
                    .id(2L)
                    .tenderId(TENDER_ID)
                    .category(ScoreCategory.FINANCIAL)
                    .weight(new BigDecimal("40.0"))
                    .passThreshold(new BigDecimal("4.0"))
                    .mandatory(false)
                    .description("Financial evaluation")
                    .build();

            when(categoryConfigRepository.findByTenderId(TENDER_ID))
                    .thenReturn(List.of(config1, config2));

            // Act
            List<EvaluationCategoryConfigDTO> result =
                    multiCriteriaEvaluationService.getCategoryConfigs(TENDER_ID);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCategory()).isEqualTo(ScoreCategory.TECHNICAL);
            assertThat(result.get(0).getWeight()).isEqualByComparingTo(new BigDecimal("60.0"));
            assertThat(result.get(0).getMandatory()).isTrue();
            assertThat(result.get(1).getCategory()).isEqualTo(ScoreCategory.FINANCIAL);
            assertThat(result.get(1).getWeight()).isEqualByComparingTo(new BigDecimal("40.0"));
            assertThat(result.get(1).getMandatory()).isFalse();
            verify(categoryConfigRepository).findByTenderId(TENDER_ID);
        }

        @Test
        @DisplayName("should return empty list when no configs exist")
        void shouldReturnEmptyListWhenNoConfigs() {
            // Arrange
            when(categoryConfigRepository.findByTenderId(TENDER_ID))
                    .thenReturn(Collections.emptyList());

            // Act
            List<EvaluationCategoryConfigDTO> result =
                    multiCriteriaEvaluationService.getCategoryConfigs(TENDER_ID);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMultiCriteriaResult")
    class GetMultiCriteriaResult {

        @Test
        @DisplayName("should return multi-criteria result with existing summaries")
        void shouldReturnMultiCriteriaResultWithExistingSummaries() {
            // Arrange
            Evaluation evaluation = Evaluation.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .evaluatorId(100L)
                    .status(EvaluationStatus.COMPLETED)
                    .overallScore(new BigDecimal("7.50"))
                    .criteriaScores(new ArrayList<>())
                    .build();

            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.of(evaluation));

            EvaluationScoreSummary techSummary = EvaluationScoreSummary.builder()
                    .id(1L)
                    .evaluationId(EVALUATION_ID)
                    .category(ScoreCategory.TECHNICAL)
                    .categoryWeight(new BigDecimal("60.0"))
                    .rawScore(new BigDecimal("8.00"))
                    .weightedScore(new BigDecimal("4.80"))
                    .maxPossibleScore(new BigDecimal("10"))
                    .criteriaCount(1)
                    .passThreshold(new BigDecimal("5.0"))
                    .passed(true)
                    .build();

            EvaluationScoreSummary financialSummary = EvaluationScoreSummary.builder()
                    .id(2L)
                    .evaluationId(EVALUATION_ID)
                    .category(ScoreCategory.FINANCIAL)
                    .categoryWeight(new BigDecimal("40.0"))
                    .rawScore(new BigDecimal("7.00"))
                    .weightedScore(new BigDecimal("2.80"))
                    .maxPossibleScore(new BigDecimal("10"))
                    .criteriaCount(1)
                    .passThreshold(new BigDecimal("4.0"))
                    .passed(true)
                    .build();

            when(scoreSummaryRepository.findByEvaluationId(EVALUATION_ID))
                    .thenReturn(List.of(techSummary, financialSummary));

            // Category configs: TECHNICAL is mandatory
            EvaluationCategoryConfig techConfig = EvaluationCategoryConfig.builder()
                    .id(1L)
                    .tenderId(TENDER_ID)
                    .category(ScoreCategory.TECHNICAL)
                    .weight(new BigDecimal("60.0"))
                    .mandatory(true)
                    .build();

            EvaluationCategoryConfig finConfig = EvaluationCategoryConfig.builder()
                    .id(2L)
                    .tenderId(TENDER_ID)
                    .category(ScoreCategory.FINANCIAL)
                    .weight(new BigDecimal("40.0"))
                    .mandatory(false)
                    .build();

            when(categoryConfigRepository.findByTenderId(TENDER_ID))
                    .thenReturn(List.of(techConfig, finConfig));

            BidDTO bidDTO = BidDTO.builder()
                    .id(BID_ID)
                    .tendererName("Test Bidder")
                    .build();
            when(bidClient.getBidById(BID_ID)).thenReturn(bidDTO);

            // Act
            MultiCriteriaEvaluationResult result =
                    multiCriteriaEvaluationService.getMultiCriteriaResult(EVALUATION_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getEvaluationId()).isEqualTo(EVALUATION_ID);
            assertThat(result.getBidId()).isEqualTo(BID_ID);
            assertThat(result.getBidderName()).isEqualTo("Test Bidder");
            assertThat(result.getOverallScore()).isEqualByComparingTo(new BigDecimal("7.50"));
            assertThat(result.getCategoryBreakdown()).hasSize(2);
            assertThat(result.isAllMandatoryCategoriesPassed()).isTrue();
            assertThat(result.isQualified()).isTrue();
        }

        @Test
        @DisplayName("should mark as not qualified when mandatory category fails")
        void shouldMarkNotQualifiedWhenMandatoryCategoryFails() {
            // Arrange
            Evaluation evaluation = Evaluation.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .evaluatorId(100L)
                    .status(EvaluationStatus.COMPLETED)
                    .overallScore(new BigDecimal("5.00"))
                    .criteriaScores(new ArrayList<>())
                    .build();

            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.of(evaluation));

            // Technical summary: failed (raw score below threshold)
            EvaluationScoreSummary techSummary = EvaluationScoreSummary.builder()
                    .id(1L)
                    .evaluationId(EVALUATION_ID)
                    .category(ScoreCategory.TECHNICAL)
                    .categoryWeight(new BigDecimal("60.0"))
                    .rawScore(new BigDecimal("3.00"))
                    .weightedScore(new BigDecimal("1.80"))
                    .maxPossibleScore(new BigDecimal("10"))
                    .criteriaCount(1)
                    .passThreshold(new BigDecimal("5.0"))
                    .passed(false)
                    .build();

            EvaluationScoreSummary financialSummary = EvaluationScoreSummary.builder()
                    .id(2L)
                    .evaluationId(EVALUATION_ID)
                    .category(ScoreCategory.FINANCIAL)
                    .categoryWeight(new BigDecimal("40.0"))
                    .rawScore(new BigDecimal("8.00"))
                    .weightedScore(new BigDecimal("3.20"))
                    .maxPossibleScore(new BigDecimal("10"))
                    .criteriaCount(1)
                    .passThreshold(new BigDecimal("4.0"))
                    .passed(true)
                    .build();

            when(scoreSummaryRepository.findByEvaluationId(EVALUATION_ID))
                    .thenReturn(List.of(techSummary, financialSummary));

            // TECHNICAL is mandatory
            EvaluationCategoryConfig techConfig = EvaluationCategoryConfig.builder()
                    .category(ScoreCategory.TECHNICAL)
                    .weight(new BigDecimal("60.0"))
                    .mandatory(true)
                    .build();

            EvaluationCategoryConfig finConfig = EvaluationCategoryConfig.builder()
                    .category(ScoreCategory.FINANCIAL)
                    .weight(new BigDecimal("40.0"))
                    .mandatory(false)
                    .build();

            when(categoryConfigRepository.findByTenderId(TENDER_ID))
                    .thenReturn(List.of(techConfig, finConfig));

            BidDTO bidDTO = BidDTO.builder()
                    .id(BID_ID)
                    .tendererName("Failing Bidder")
                    .build();
            when(bidClient.getBidById(BID_ID)).thenReturn(bidDTO);

            // Act
            MultiCriteriaEvaluationResult result =
                    multiCriteriaEvaluationService.getMultiCriteriaResult(EVALUATION_ID);

            // Assert
            assertThat(result.isAllMandatoryCategoriesPassed()).isFalse();
            assertThat(result.isQualified()).isFalse();
        }

        @Test
        @DisplayName("should throw when evaluation not found")
        void shouldThrowWhenEvaluationNotFound() {
            // Arrange
            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    multiCriteriaEvaluationService.getMultiCriteriaResult(EVALUATION_ID))
                    .isInstanceOf(EvaluationNotFoundException.class);
        }

        @Test
        @DisplayName("should compute score breakdown when no summaries exist yet")
        void shouldComputeScoreBreakdownWhenNoSummariesExist() {
            // Arrange
            Evaluation evaluation = Evaluation.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .evaluatorId(100L)
                    .status(EvaluationStatus.COMPLETED)
                    .overallScore(new BigDecimal("7.00"))
                    .criteriaScores(new ArrayList<>())
                    .build();

            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.of(evaluation));

            // No existing summaries -- triggers computeScoreBreakdown
            when(scoreSummaryRepository.findByEvaluationId(EVALUATION_ID))
                    .thenReturn(Collections.emptyList());

            // computeScoreBreakdown will look up configs
            when(categoryConfigRepository.findByTenderId(TENDER_ID))
                    .thenReturn(Collections.emptyList());

            BidDTO bidDTO = BidDTO.builder()
                    .id(BID_ID)
                    .tendererName("Test Bidder")
                    .build();
            when(bidClient.getBidById(BID_ID)).thenReturn(bidDTO);

            // Act
            MultiCriteriaEvaluationResult result =
                    multiCriteriaEvaluationService.getMultiCriteriaResult(EVALUATION_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getEvaluationId()).isEqualTo(EVALUATION_ID);
            // With empty configs, breakdown will be empty, all mandatory passed = true (vacuous truth)
            assertThat(result.getCategoryBreakdown()).isEmpty();
            assertThat(result.isAllMandatoryCategoriesPassed()).isTrue();
            assertThat(result.isQualified()).isTrue();
        }
    }
}
