package com.egov.tendering.evaluation.service.impl;

import com.egov.tendering.dto.BidDTO;
import com.egov.tendering.dto.TenderCriteriaDTO;
import com.egov.tendering.dto.TenderDTO;
import com.egov.tendering.evaluation.client.BidClient;
import com.egov.tendering.evaluation.client.TenderClient;
import com.egov.tendering.evaluation.dal.dto.CriteriaScoreDTO;
import com.egov.tendering.evaluation.dal.dto.CriteriaScoreRequest;
import com.egov.tendering.evaluation.dal.dto.EvaluationDTO;
import com.egov.tendering.evaluation.dal.dto.EvaluationRequest;
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
import com.egov.tendering.evaluation.service.TenderRankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private CriteriaScoreRepository criteriaScoreRepository;

    @Mock
    private EvaluationMapper evaluationMapper;

    @Mock
    private CriteriaScoreMapper criteriaScoreMapper;

    @Mock
    private EvaluationEventPublisher eventPublisher;

    @Mock
    private TenderClient tenderClient;

    @Mock
    private BidClient bidClient;

    @Mock
    private TenderRankingService rankingService;

    @Mock
    private AllocationService allocationService;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    private static final Long TENDER_ID = 1L;
    private static final Long BID_ID = 10L;
    private static final Long EVALUATOR_ID = 100L;
    private static final Long EVALUATION_ID = 50L;

    private TenderDTO tenderDTO;
    private BidDTO bidDTO;
    private EvaluationRequest evaluationRequest;

    @BeforeEach
    void setUp() {
        TenderCriteriaDTO criteria1 = TenderCriteriaDTO.builder()
                .id(1L)
                .name("Price")
                .weight(new BigDecimal("60"))
                .criteriaType("PRICE")
                .build();

        TenderCriteriaDTO criteria2 = TenderCriteriaDTO.builder()
                .id(2L)
                .name("Quality")
                .weight(new BigDecimal("40"))
                .criteriaType("QUALITY")
                .build();

        tenderDTO = TenderDTO.builder()
                .id(TENDER_ID)
                .title("Test Tender")
                .criteria(List.of(criteria1, criteria2))
                .build();

        bidDTO = BidDTO.builder()
                .id(BID_ID)
                .tenderId(TENDER_ID)
                .tendererName("Test Bidder")
                .build();

        CriteriaScoreRequest scoreReq1 = CriteriaScoreRequest.builder()
                .criteriaId(1L)
                .score(new BigDecimal("8.00"))
                .justification("Good price")
                .build();

        CriteriaScoreRequest scoreReq2 = CriteriaScoreRequest.builder()
                .criteriaId(2L)
                .score(new BigDecimal("7.00"))
                .justification("Good quality")
                .build();

        evaluationRequest = EvaluationRequest.builder()
                .bidId(BID_ID)
                .criteriaScores(List.of(scoreReq1, scoreReq2))
                .comments("Overall good bid")
                .build();
    }

    @Nested
    @DisplayName("createEvaluation")
    class CreateEvaluation {

        @Test
        @DisplayName("should create evaluation with weighted score calculation")
        void shouldCreateEvaluationWithWeightedScoreCalculation() {
            // Arrange
            when(evaluationRepository.findByBidIdAndEvaluatorId(BID_ID, EVALUATOR_ID))
                    .thenReturn(Optional.empty());

            Evaluation savedEvaluation = Evaluation.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .evaluatorId(EVALUATOR_ID)
                    .status(EvaluationStatus.IN_PROGRESS)
                    .comments("Overall good bid")
                    .criteriaScores(new ArrayList<>())
                    .build();

            when(evaluationRepository.save(any(Evaluation.class))).thenReturn(savedEvaluation);
            when(tenderClient.getTenderById(TENDER_ID)).thenReturn(tenderDTO);

            List<CriteriaScore> savedScores = List.of(
                    CriteriaScore.builder().id(1L).criteriaId(1L).score(new BigDecimal("8.00")).build(),
                    CriteriaScore.builder().id(2L).criteriaId(2L).score(new BigDecimal("7.00")).build()
            );
            when(criteriaScoreRepository.saveAll(anyList())).thenReturn(savedScores);

            EvaluationDTO expectedDTO = EvaluationDTO.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .evaluatorId(EVALUATOR_ID)
                    .status(EvaluationStatus.IN_PROGRESS)
                    .comments("Overall good bid")
                    .criteriaScores(List.of(
                            CriteriaScoreDTO.builder().criteriaId(1L).score(new BigDecimal("8.00")).build(),
                            CriteriaScoreDTO.builder().criteriaId(2L).score(new BigDecimal("7.00")).build()
                    ))
                    .build();
            when(evaluationMapper.toDto(any(Evaluation.class))).thenReturn(expectedDTO);
            when(bidClient.getBidById(BID_ID)).thenReturn(bidDTO);

            // Act
            EvaluationDTO result = evaluationService.createEvaluation(TENDER_ID, evaluationRequest, EVALUATOR_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTenderId()).isEqualTo(TENDER_ID);
            assertThat(result.getBidId()).isEqualTo(BID_ID);
            assertThat(result.getBidderName()).isEqualTo("Test Bidder");

            // Verify weighted score calculation:
            // score1=8 * weight1=60 + score2=7 * weight2=40 = 480 + 280 = 760
            // totalWeight = 60 + 40 = 100
            // overallScore = 760 / 100 = 7.60
            ArgumentCaptor<Evaluation> evalCaptor = ArgumentCaptor.forClass(Evaluation.class);
            verify(evaluationRepository, atLeast(2)).save(evalCaptor.capture());
            List<Evaluation> capturedEvals = evalCaptor.getAllValues();
            Evaluation lastSaved = capturedEvals.get(capturedEvals.size() - 1);
            assertThat(lastSaved.getOverallScore()).isEqualByComparingTo(new BigDecimal("7.60"));

            verify(eventPublisher).publishEvaluationCreatedEvent(any(Evaluation.class));
        }

        @Test
        @DisplayName("should throw when evaluation already exists for bid and evaluator")
        void shouldThrowWhenEvaluationAlreadyExists() {
            // Arrange
            Evaluation existingEvaluation = Evaluation.builder()
                    .id(EVALUATION_ID)
                    .bidId(BID_ID)
                    .evaluatorId(EVALUATOR_ID)
                    .build();
            when(evaluationRepository.findByBidIdAndEvaluatorId(BID_ID, EVALUATOR_ID))
                    .thenReturn(Optional.of(existingEvaluation));

            // Act & Assert
            assertThatThrownBy(() ->
                    evaluationService.createEvaluation(TENDER_ID, evaluationRequest, EVALUATOR_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Evaluation already exists");

            verify(evaluationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateEvaluation")
    class UpdateEvaluation {

        @Test
        @DisplayName("should update evaluation successfully when not completed")
        void shouldUpdateEvaluationSuccessfully() {
            // Arrange
            Evaluation existingEvaluation = Evaluation.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .evaluatorId(EVALUATOR_ID)
                    .status(EvaluationStatus.IN_PROGRESS)
                    .criteriaScores(new ArrayList<>())
                    .build();

            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.of(existingEvaluation));
            when(tenderClient.getTenderById(TENDER_ID)).thenReturn(tenderDTO);

            List<CriteriaScore> savedScores = List.of(
                    CriteriaScore.builder().id(1L).criteriaId(1L).score(new BigDecimal("8.00")).build(),
                    CriteriaScore.builder().id(2L).criteriaId(2L).score(new BigDecimal("7.00")).build()
            );
            when(criteriaScoreRepository.saveAll(anyList())).thenReturn(savedScores);
            when(evaluationRepository.save(any(Evaluation.class))).thenReturn(existingEvaluation);

            EvaluationDTO expectedDTO = EvaluationDTO.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .status(EvaluationStatus.IN_PROGRESS)
                    .criteriaScores(List.of())
                    .build();
            when(evaluationMapper.toDto(any(Evaluation.class))).thenReturn(expectedDTO);
            when(bidClient.getBidById(BID_ID)).thenReturn(bidDTO);

            // Act
            EvaluationDTO result = evaluationService.updateEvaluation(EVALUATION_ID, evaluationRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(evaluationRepository).save(any(Evaluation.class));
            verify(eventPublisher).publishEvaluationUpdatedEvent(any(Evaluation.class));
        }

        @Test
        @DisplayName("should throw when updating a completed evaluation")
        void shouldThrowWhenUpdatingCompletedEvaluation() {
            // Arrange
            Evaluation completedEvaluation = Evaluation.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .status(EvaluationStatus.COMPLETED)
                    .criteriaScores(new ArrayList<>())
                    .build();

            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.of(completedEvaluation));

            // Act & Assert
            assertThatThrownBy(() ->
                    evaluationService.updateEvaluation(EVALUATION_ID, evaluationRequest))
                    .isInstanceOf(InvalidEvaluationStateException.class)
                    .hasMessageContaining("Cannot update a completed evaluation");

            verify(evaluationRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvaluationUpdatedEvent(any());
        }

        @Test
        @DisplayName("should throw when evaluation not found")
        void shouldThrowWhenEvaluationNotFound() {
            // Arrange
            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    evaluationService.updateEvaluation(EVALUATION_ID, evaluationRequest))
                    .isInstanceOf(EvaluationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateEvaluationStatus")
    class UpdateEvaluationStatus {

        @Test
        @DisplayName("should update evaluation status successfully")
        void shouldUpdateStatusSuccessfully() {
            // Arrange
            Evaluation evaluation = Evaluation.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .evaluatorId(EVALUATOR_ID)
                    .status(EvaluationStatus.IN_PROGRESS)
                    .criteriaScores(new ArrayList<>())
                    .build();

            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.of(evaluation));

            Evaluation updatedEvaluation = Evaluation.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .evaluatorId(EVALUATOR_ID)
                    .status(EvaluationStatus.COMPLETED)
                    .criteriaScores(new ArrayList<>())
                    .build();
            when(evaluationRepository.save(any(Evaluation.class))).thenReturn(updatedEvaluation);

            EvaluationDTO expectedDTO = EvaluationDTO.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .status(EvaluationStatus.COMPLETED)
                    .criteriaScores(List.of())
                    .build();
            when(evaluationMapper.toDto(any(Evaluation.class))).thenReturn(expectedDTO);
            when(bidClient.getBidById(BID_ID)).thenReturn(bidDTO);
            when(tenderClient.getTenderById(TENDER_ID)).thenReturn(tenderDTO);

            // Act
            EvaluationDTO result = evaluationService.updateEvaluationStatus(EVALUATION_ID, EvaluationStatus.COMPLETED);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
            verify(eventPublisher).publishEvaluationStatusChangedEvent(
                    any(Evaluation.class), eq(EvaluationStatus.IN_PROGRESS));
        }

        @Test
        @DisplayName("should throw when evaluation not found for status update")
        void shouldThrowWhenEvaluationNotFoundForStatusUpdate() {
            // Arrange
            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    evaluationService.updateEvaluationStatus(EVALUATION_ID, EvaluationStatus.COMPLETED))
                    .isInstanceOf(EvaluationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteEvaluation")
    class DeleteEvaluation {

        @Test
        @DisplayName("should delete evaluation successfully when not completed")
        void shouldDeleteEvaluationSuccessfully() {
            // Arrange
            Evaluation evaluation = Evaluation.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .evaluatorId(EVALUATOR_ID)
                    .status(EvaluationStatus.IN_PROGRESS)
                    .criteriaScores(new ArrayList<>())
                    .build();

            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.of(evaluation));

            // Act
            evaluationService.deleteEvaluation(EVALUATION_ID);

            // Assert
            verify(eventPublisher).publishEvaluationDeletedEvent(evaluation);
            verify(evaluationRepository).delete(evaluation);
        }

        @Test
        @DisplayName("should throw when deleting a completed evaluation")
        void shouldThrowWhenDeletingCompletedEvaluation() {
            // Arrange
            Evaluation completedEvaluation = Evaluation.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .status(EvaluationStatus.COMPLETED)
                    .criteriaScores(new ArrayList<>())
                    .build();

            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.of(completedEvaluation));

            // Act & Assert
            assertThatThrownBy(() -> evaluationService.deleteEvaluation(EVALUATION_ID))
                    .isInstanceOf(InvalidEvaluationStateException.class)
                    .hasMessageContaining("Cannot delete a completed evaluation");

            verify(evaluationRepository, never()).delete(any());
            verify(eventPublisher, never()).publishEvaluationDeletedEvent(any());
        }

        @Test
        @DisplayName("should allow deleting a pending evaluation")
        void shouldAllowDeletingPendingEvaluation() {
            // Arrange
            Evaluation pendingEvaluation = Evaluation.builder()
                    .id(EVALUATION_ID)
                    .tenderId(TENDER_ID)
                    .bidId(BID_ID)
                    .evaluatorId(EVALUATOR_ID)
                    .status(EvaluationStatus.PENDING)
                    .criteriaScores(new ArrayList<>())
                    .build();

            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.of(pendingEvaluation));

            // Act
            evaluationService.deleteEvaluation(EVALUATION_ID);

            // Assert
            verify(eventPublisher).publishEvaluationDeletedEvent(pendingEvaluation);
            verify(evaluationRepository).delete(pendingEvaluation);
        }

        @Test
        @DisplayName("should throw when evaluation not found for deletion")
        void shouldThrowWhenEvaluationNotFoundForDeletion() {
            // Arrange
            when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> evaluationService.deleteEvaluation(EVALUATION_ID))
                    .isInstanceOf(EvaluationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getEvaluationsByTender")
    class GetEvaluationsByTender {

        @Test
        @DisplayName("should return evaluations for a tender")
        void shouldReturnEvaluationsForTender() {
            // Arrange
            Evaluation eval1 = Evaluation.builder()
                    .id(1L)
                    .tenderId(TENDER_ID)
                    .bidId(10L)
                    .evaluatorId(EVALUATOR_ID)
                    .status(EvaluationStatus.IN_PROGRESS)
                    .criteriaScores(new ArrayList<>())
                    .build();

            Evaluation eval2 = Evaluation.builder()
                    .id(2L)
                    .tenderId(TENDER_ID)
                    .bidId(20L)
                    .evaluatorId(EVALUATOR_ID)
                    .status(EvaluationStatus.COMPLETED)
                    .criteriaScores(new ArrayList<>())
                    .build();

            List<Evaluation> evaluations = List.of(eval1, eval2);
            when(evaluationRepository.findByTenderId(TENDER_ID)).thenReturn(evaluations);

            EvaluationDTO dto1 = EvaluationDTO.builder()
                    .id(1L).tenderId(TENDER_ID).bidId(10L)
                    .status(EvaluationStatus.IN_PROGRESS)
                    .criteriaScores(List.of())
                    .build();
            EvaluationDTO dto2 = EvaluationDTO.builder()
                    .id(2L).tenderId(TENDER_ID).bidId(20L)
                    .status(EvaluationStatus.COMPLETED)
                    .criteriaScores(List.of())
                    .build();

            when(evaluationMapper.toDtoList(evaluations)).thenReturn(new ArrayList<>(List.of(dto1, dto2)));

            BidDTO bidDTO1 = BidDTO.builder().id(10L).tendererName("Bidder 1").build();
            BidDTO bidDTO2 = BidDTO.builder().id(20L).tendererName("Bidder 2").build();
            when(bidClient.getBidById(10L)).thenReturn(bidDTO1);
            when(bidClient.getBidById(20L)).thenReturn(bidDTO2);
            when(tenderClient.getTenderById(TENDER_ID)).thenReturn(tenderDTO);

            // Act
            List<EvaluationDTO> result = evaluationService.getEvaluationsByTender(TENDER_ID);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getBidderName()).isEqualTo("Bidder 1");
            assertThat(result.get(1).getBidderName()).isEqualTo("Bidder 2");
            verify(evaluationRepository).findByTenderId(TENDER_ID);
        }

        @Test
        @DisplayName("should return empty list when no evaluations exist for tender")
        void shouldReturnEmptyListWhenNoEvaluations() {
            // Arrange
            when(evaluationRepository.findByTenderId(TENDER_ID)).thenReturn(Collections.emptyList());
            when(evaluationMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            // Act
            List<EvaluationDTO> result = evaluationService.getEvaluationsByTender(TENDER_ID);

            // Assert
            assertThat(result).isEmpty();
        }
    }
}
