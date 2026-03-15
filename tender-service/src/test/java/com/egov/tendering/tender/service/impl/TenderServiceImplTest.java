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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenderServiceImplTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private TenderCriteriaRepository criteriaRepository;

    @Mock
    private TenderItemRepository itemRepository;

    @Mock
    private TenderAmendmentRepository amendmentRepository;

    @Mock
    private TenderMapper tenderMapper;

    @Mock
    private TenderEventPublisher eventPublisher;

    @InjectMocks
    private TenderServiceImpl tenderService;

    @Captor
    private ArgumentCaptor<Tender> tenderCaptor;

    @Captor
    private ArgumentCaptor<TenderAmendment> amendmentCaptor;

    private Tender sampleTender;
    private TenderDTO sampleTenderDTO;
    private final Long TENDER_ID = 1L;
    private final Long TENDEREE_ID = 100L;

    @BeforeEach
    void setUp() {
        sampleTender = Tender.builder()
                .id(TENDER_ID)
                .title("Test Tender")
                .description("Test Description")
                .tendereeId(TENDEREE_ID)
                .type(TenderType.OPEN)
                .status(TenderStatus.DRAFT)
                .submissionDeadline(LocalDateTime.now().plusDays(30))
                .allocationStrategy(AllocationStrategy.SINGLE)
                .minWinners(1)
                .maxWinners(3)
                .cutoffScore(BigDecimal.valueOf(70))
                .isAverageAllocation(false)
                .build();

        sampleTenderDTO = TenderDTO.builder()
                .id(TENDER_ID)
                .title("Test Tender")
                .description("Test Description")
                .tendereeId(TENDEREE_ID)
                .type(TenderType.OPEN)
                .status(TenderStatus.DRAFT)
                .submissionDeadline(sampleTender.getSubmissionDeadline())
                .allocationStrategy(AllocationStrategy.SINGLE)
                .minWinners(1)
                .maxWinners(3)
                .cutoffScore(BigDecimal.valueOf(70))
                .isAverageAllocation(false)
                .build();
    }

    @Nested
    @DisplayName("createTender")
    class CreateTenderTests {

        @Test
        @DisplayName("should create tender successfully with criteria and items")
        void createTender_Success() {
            // Arrange
            TenderCriteriaRequest criteriaRequest = TenderCriteriaRequest.builder()
                    .name("Price Criteria")
                    .description("Price evaluation")
                    .type(CriteriaType.PRICE)
                    .weight(BigDecimal.valueOf(60))
                    .preferHigher(false)
                    .build();

            TenderItemRequest itemRequest = TenderItemRequest.builder()
                    .criteriaId(10L)
                    .name("Item 1")
                    .description("First item")
                    .quantity(100)
                    .unit("pieces")
                    .estimatedPrice(BigDecimal.valueOf(5000))
                    .build();

            CreateTenderRequest request = CreateTenderRequest.builder()
                    .title("Test Tender")
                    .description("Test Description")
                    .type(TenderType.OPEN)
                    .submissionDeadline(LocalDateTime.now().plusDays(30))
                    .allocationStrategy(AllocationStrategy.SINGLE)
                    .minWinners(1)
                    .maxWinners(3)
                    .cutoffScore(BigDecimal.valueOf(70))
                    .isAverageAllocation(false)
                    .criteria(List.of(criteriaRequest))
                    .items(List.of(itemRequest))
                    .build();

            TenderCriteria savedCriteria = TenderCriteria.builder()
                    .id(10L)
                    .tender(sampleTender)
                    .name("Price Criteria")
                    .description("Price evaluation")
                    .type(CriteriaType.PRICE)
                    .weight(BigDecimal.valueOf(60))
                    .preferHigher(false)
                    .build();

            TenderItem savedItem = TenderItem.builder()
                    .id(20L)
                    .tender(sampleTender)
                    .criteria(savedCriteria)
                    .name("Item 1")
                    .description("First item")
                    .quantity(100)
                    .unit("pieces")
                    .estimatedPrice(BigDecimal.valueOf(5000))
                    .build();

            when(tenderRepository.save(any(Tender.class))).thenReturn(sampleTender);
            when(criteriaRepository.saveAll(anyList())).thenReturn(List.of(savedCriteria));
            when(itemRepository.saveAll(anyList())).thenReturn(List.of(savedItem));
            when(tenderMapper.toDto(any(Tender.class))).thenReturn(sampleTenderDTO);

            // Act
            TenderDTO result = tenderService.createTender(request, TENDEREE_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Test Tender");
            assertThat(result.getStatus()).isEqualTo(TenderStatus.DRAFT);

            verify(tenderRepository).save(any(Tender.class));
            verify(criteriaRepository).saveAll(anyList());
            verify(itemRepository).saveAll(anyList());
            verify(eventPublisher).publishTenderCreatedEvent(any(Tender.class));
        }

        @Test
        @DisplayName("should set initial status to DRAFT")
        void createTender_ShouldSetDraftStatus() {
            // Arrange
            TenderCriteriaRequest criteriaRequest = TenderCriteriaRequest.builder()
                    .name("Quality")
                    .type(CriteriaType.QUALITY)
                    .weight(BigDecimal.valueOf(40))
                    .build();

            TenderCriteria savedCriteria = TenderCriteria.builder()
                    .id(10L)
                    .tender(sampleTender)
                    .name("Quality")
                    .type(CriteriaType.QUALITY)
                    .weight(BigDecimal.valueOf(40))
                    .build();

            TenderItemRequest itemRequest = TenderItemRequest.builder()
                    .criteriaId(10L)
                    .name("Item 1")
                    .quantity(1)
                    .build();

            CreateTenderRequest request = CreateTenderRequest.builder()
                    .title("Draft Tender")
                    .type(TenderType.OPEN)
                    .submissionDeadline(LocalDateTime.now().plusDays(10))
                    .allocationStrategy(AllocationStrategy.SINGLE)
                    .criteria(List.of(criteriaRequest))
                    .items(List.of(itemRequest))
                    .build();

            when(tenderRepository.save(any(Tender.class))).thenReturn(sampleTender);
            when(criteriaRepository.saveAll(anyList())).thenReturn(List.of(savedCriteria));
            when(itemRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
            when(tenderMapper.toDto(any(Tender.class))).thenReturn(sampleTenderDTO);

            // Act
            tenderService.createTender(request, TENDEREE_ID);

            // Assert
            verify(tenderRepository).save(tenderCaptor.capture());
            Tender captured = tenderCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(TenderStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("getTenderById")
    class GetTenderByIdTests {

        @Test
        @DisplayName("should return tender when found")
        void getTenderById_Success() {
            // Arrange
            when(tenderRepository.findById(TENDER_ID)).thenReturn(Optional.of(sampleTender));
            when(tenderMapper.toDto(sampleTender)).thenReturn(sampleTenderDTO);

            // Act
            TenderDTO result = tenderService.getTenderById(TENDER_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TENDER_ID);
            assertThat(result.getTitle()).isEqualTo("Test Tender");
            verify(tenderRepository).findById(TENDER_ID);
        }

        @Test
        @DisplayName("should throw TenderNotFoundException when not found")
        void getTenderById_NotFound() {
            // Arrange
            when(tenderRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> tenderService.getTenderById(999L))
                    .isInstanceOf(TenderNotFoundException.class)
                    .hasMessageContaining("Tender not found with ID: 999");
        }
    }

    @Nested
    @DisplayName("publishTender")
    class PublishTenderTests {

        @Test
        @DisplayName("should publish tender in DRAFT status successfully")
        void publishTender_Success() {
            // Arrange
            sampleTender.setStatus(TenderStatus.DRAFT);
            Tender publishedTender = Tender.builder()
                    .id(TENDER_ID)
                    .title("Test Tender")
                    .status(TenderStatus.PUBLISHED)
                    .build();

            TenderDTO publishedDTO = TenderDTO.builder()
                    .id(TENDER_ID)
                    .title("Test Tender")
                    .status(TenderStatus.PUBLISHED)
                    .build();

            when(tenderRepository.findById(TENDER_ID)).thenReturn(Optional.of(sampleTender));
            when(tenderRepository.save(any(Tender.class))).thenReturn(publishedTender);
            when(tenderMapper.toDto(any(Tender.class))).thenReturn(publishedDTO);

            // Act
            TenderDTO result = tenderService.publishTender(TENDER_ID);

            // Assert
            assertThat(result.getStatus()).isEqualTo(TenderStatus.PUBLISHED);
            verify(tenderRepository).save(tenderCaptor.capture());
            assertThat(tenderCaptor.getValue().getStatus()).isEqualTo(TenderStatus.PUBLISHED);
            verify(eventPublisher).publishTenderPublishedEvent(any(Tender.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException when tender is not in DRAFT status")
        void publishTender_InvalidState() {
            // Arrange
            sampleTender.setStatus(TenderStatus.PUBLISHED);
            when(tenderRepository.findById(TENDER_ID)).thenReturn(Optional.of(sampleTender));

            // Act & Assert
            assertThatThrownBy(() -> tenderService.publishTender(TENDER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only tenders in DRAFT status can be published");

            verify(tenderRepository, never()).save(any(Tender.class));
            verify(eventPublisher, never()).publishTenderPublishedEvent(any(Tender.class));
        }

        @Test
        @DisplayName("should throw TenderNotFoundException when tender does not exist")
        void publishTender_TenderNotFound() {
            // Arrange
            when(tenderRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> tenderService.publishTender(999L))
                    .isInstanceOf(TenderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("closeTender")
    class CloseTenderTests {

        @Test
        @DisplayName("should close a published tender successfully")
        void closeTender_Success() {
            // Arrange
            sampleTender.setStatus(TenderStatus.PUBLISHED);
            Tender closedTender = Tender.builder()
                    .id(TENDER_ID)
                    .title("Test Tender")
                    .status(TenderStatus.CLOSED)
                    .build();

            TenderDTO closedDTO = TenderDTO.builder()
                    .id(TENDER_ID)
                    .status(TenderStatus.CLOSED)
                    .build();

            when(tenderRepository.findById(TENDER_ID)).thenReturn(Optional.of(sampleTender));
            when(tenderRepository.save(any(Tender.class))).thenReturn(closedTender);
            when(tenderMapper.toDto(any(Tender.class))).thenReturn(closedDTO);

            // Act
            TenderDTO result = tenderService.closeTender(TENDER_ID);

            // Assert
            assertThat(result.getStatus()).isEqualTo(TenderStatus.CLOSED);
            verify(tenderRepository).save(tenderCaptor.capture());
            assertThat(tenderCaptor.getValue().getStatus()).isEqualTo(TenderStatus.CLOSED);
            verify(eventPublisher).publishTenderClosedEvent(any(Tender.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException when tender is not PUBLISHED")
        void closeTender_InvalidState() {
            // Arrange
            sampleTender.setStatus(TenderStatus.DRAFT);
            when(tenderRepository.findById(TENDER_ID)).thenReturn(Optional.of(sampleTender));

            // Act & Assert
            assertThatThrownBy(() -> tenderService.closeTender(TENDER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only published tenders can be closed");

            verify(tenderRepository, never()).save(any(Tender.class));
            verify(eventPublisher, never()).publishTenderClosedEvent(any(Tender.class));
        }
    }

    @Nested
    @DisplayName("amendTender")
    class AmendTenderTests {

        private TenderAmendmentRequest amendmentRequest;
        private final Long AMENDED_BY = 200L;

        @BeforeEach
        void setUp() {
            amendmentRequest = TenderAmendmentRequest.builder()
                    .reason("Extended deadline")
                    .description("Updated description")
                    .newSubmissionDeadline(LocalDateTime.now().plusDays(60))
                    .build();
        }

        @Test
        @DisplayName("should amend a published tender successfully")
        void amendTender_Success_Published() {
            // Arrange
            sampleTender.setStatus(TenderStatus.PUBLISHED);
            sampleTender.setSubmissionDeadline(LocalDateTime.now().plusDays(30));
            sampleTender.setDescription("Old Description");

            TenderDTO amendedDTO = TenderDTO.builder()
                    .id(TENDER_ID)
                    .status(TenderStatus.AMENDED)
                    .description("Updated description")
                    .build();

            when(tenderRepository.findById(TENDER_ID)).thenReturn(Optional.of(sampleTender));
            when(amendmentRepository.findMaxAmendmentNumber(TENDER_ID)).thenReturn(0);
            when(tenderRepository.save(any(Tender.class))).thenReturn(sampleTender);
            when(amendmentRepository.save(any(TenderAmendment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(tenderMapper.toDto(any(Tender.class))).thenReturn(amendedDTO);

            // Act
            TenderDTO result = tenderService.amendTender(TENDER_ID, amendmentRequest, AMENDED_BY);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TenderStatus.AMENDED);

            verify(tenderRepository).save(tenderCaptor.capture());
            Tender capturedTender = tenderCaptor.getValue();
            assertThat(capturedTender.getStatus()).isEqualTo(TenderStatus.AMENDED);
            assertThat(capturedTender.getDescription()).isEqualTo("Updated description");
            assertThat(capturedTender.getSubmissionDeadline()).isEqualTo(amendmentRequest.getNewSubmissionDeadline());

            verify(amendmentRepository).save(amendmentCaptor.capture());
            TenderAmendment capturedAmendment = amendmentCaptor.getValue();
            assertThat(capturedAmendment.getAmendmentNumber()).isEqualTo(1);
            assertThat(capturedAmendment.getReason()).isEqualTo("Extended deadline");
            assertThat(capturedAmendment.getAmendedBy()).isEqualTo(AMENDED_BY);

            verify(eventPublisher).publishTenderAmendedEvent(any(Tender.class), any(TenderAmendment.class));
        }

        @Test
        @DisplayName("should amend an already-amended tender successfully")
        void amendTender_Success_AlreadyAmended() {
            // Arrange
            sampleTender.setStatus(TenderStatus.AMENDED);

            TenderDTO amendedDTO = TenderDTO.builder()
                    .id(TENDER_ID)
                    .status(TenderStatus.AMENDED)
                    .build();

            when(tenderRepository.findById(TENDER_ID)).thenReturn(Optional.of(sampleTender));
            when(amendmentRepository.findMaxAmendmentNumber(TENDER_ID)).thenReturn(2);
            when(tenderRepository.save(any(Tender.class))).thenReturn(sampleTender);
            when(amendmentRepository.save(any(TenderAmendment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(tenderMapper.toDto(any(Tender.class))).thenReturn(amendedDTO);

            // Act
            TenderDTO result = tenderService.amendTender(TENDER_ID, amendmentRequest, AMENDED_BY);

            // Assert
            assertThat(result).isNotNull();
            verify(amendmentRepository).save(amendmentCaptor.capture());
            assertThat(amendmentCaptor.getValue().getAmendmentNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("should throw IllegalStateException when tender is in DRAFT status")
        void amendTender_InvalidState_Draft() {
            // Arrange
            sampleTender.setStatus(TenderStatus.DRAFT);
            when(tenderRepository.findById(TENDER_ID)).thenReturn(Optional.of(sampleTender));

            // Act & Assert
            assertThatThrownBy(() -> tenderService.amendTender(TENDER_ID, amendmentRequest, AMENDED_BY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only PUBLISHED or AMENDED tenders can be amended");

            verify(tenderRepository, never()).save(any(Tender.class));
            verify(amendmentRepository, never()).save(any(TenderAmendment.class));
            verify(eventPublisher, never()).publishTenderAmendedEvent(any(Tender.class), any(TenderAmendment.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException when tender is CLOSED")
        void amendTender_InvalidState_Closed() {
            // Arrange
            sampleTender.setStatus(TenderStatus.CLOSED);
            when(tenderRepository.findById(TENDER_ID)).thenReturn(Optional.of(sampleTender));

            // Act & Assert
            assertThatThrownBy(() -> tenderService.amendTender(TENDER_ID, amendmentRequest, AMENDED_BY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only PUBLISHED or AMENDED tenders can be amended");
        }

        @Test
        @DisplayName("should throw TenderNotFoundException when tender does not exist")
        void amendTender_TenderNotFound() {
            // Arrange
            when(tenderRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> tenderService.amendTender(999L, amendmentRequest, AMENDED_BY))
                    .isInstanceOf(TenderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getTenderAmendments")
    class GetTenderAmendmentsTests {

        @Test
        @DisplayName("should return list of amendments for a tender")
        void getTenderAmendments_Success() {
            // Arrange
            TenderAmendment amendment1 = TenderAmendment.builder()
                    .id(1L)
                    .tenderId(TENDER_ID)
                    .amendmentNumber(2)
                    .reason("Second amendment")
                    .description("Second description change")
                    .previousDeadline(LocalDateTime.now().plusDays(30))
                    .newDeadline(LocalDateTime.now().plusDays(60))
                    .amendedBy(200L)
                    .createdAt(LocalDateTime.now())
                    .build();

            TenderAmendment amendment2 = TenderAmendment.builder()
                    .id(2L)
                    .tenderId(TENDER_ID)
                    .amendmentNumber(1)
                    .reason("First amendment")
                    .description("First description change")
                    .previousDeadline(LocalDateTime.now().plusDays(15))
                    .newDeadline(LocalDateTime.now().plusDays(30))
                    .amendedBy(200L)
                    .createdAt(LocalDateTime.now().minusDays(5))
                    .build();

            when(amendmentRepository.findByTenderIdOrderByAmendmentNumberDesc(TENDER_ID))
                    .thenReturn(List.of(amendment1, amendment2));

            // Act
            List<TenderAmendmentDTO> result = tenderService.getTenderAmendments(TENDER_ID);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAmendmentNumber()).isEqualTo(2);
            assertThat(result.get(0).getReason()).isEqualTo("Second amendment");
            assertThat(result.get(1).getAmendmentNumber()).isEqualTo(1);
            verify(amendmentRepository).findByTenderIdOrderByAmendmentNumberDesc(TENDER_ID);
        }

        @Test
        @DisplayName("should return empty list when no amendments exist")
        void getTenderAmendments_Empty() {
            // Arrange
            when(amendmentRepository.findByTenderIdOrderByAmendmentNumberDesc(TENDER_ID))
                    .thenReturn(Collections.emptyList());

            // Act
            List<TenderAmendmentDTO> result = tenderService.getTenderAmendments(TENDER_ID);

            // Assert
            assertThat(result).isEmpty();
        }
    }
}
