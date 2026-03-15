package com.egov.tendering.bidding.service.impl;

import com.egov.tendering.bidding.dal.dto.BidSealDTO;
import com.egov.tendering.bidding.dal.model.*;
import com.egov.tendering.bidding.dal.repository.BidRepository;
import com.egov.tendering.bidding.dal.repository.BidSealRepository;
import com.egov.tendering.bidding.service.TenderWorkflowGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidSealingServiceImplTest {

    @Mock
    private BidSealRepository bidSealRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private TenderWorkflowGuard tenderWorkflowGuard;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BidSealingServiceImpl bidSealingService;

    private Bid sampleBid;
    private BidSeal sampleSeal;
    private static final Long BID_ID = 1L;
    private static final Long TENDER_ID = 100L;
    private static final Long USER_ID = 50L;

    @BeforeEach
    void setUp() {
        sampleBid = Bid.builder()
                .id(BID_ID)
                .tenderId(TENDER_ID)
                .tendererId(200L)
                .status(BidStatus.SUBMITTED)
                .totalPrice(new BigDecimal("50000.00"))
                .submissionTime(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();

        sampleSeal = BidSeal.builder()
                .id(10L)
                .bidId(BID_ID)
                .tenderId(TENDER_ID)
                .contentHash("abc123hash")
                .encryptedContent("encryptedData")
                .encryptionAlgorithm("AES/GCM/NoPadding")
                .sealKeyReference("keyRef")
                .status(SealStatus.SEALED)
                .sealedAt(LocalDateTime.now().minusHours(1))
                .sealedBy(USER_ID)
                .scheduledUnsealTime(LocalDateTime.now().minusMinutes(30))
                .integrityVerified(true)
                .build();
    }

    @Nested
    @DisplayName("sealBid")
    class SealBidTests {

        @Test
        @DisplayName("should seal a submitted bid successfully")
        void sealBid_success() throws Exception {
            when(bidSealRepository.existsByBidId(BID_ID)).thenReturn(false);
            when(bidRepository.findById(BID_ID)).thenReturn(Optional.of(sampleBid));
            when(objectMapper.writeValueAsString(any(Bid.class))).thenReturn("{\"id\":1}");
            when(tenderWorkflowGuard.getSubmissionDeadline(TENDER_ID))
                    .thenReturn(LocalDateTime.now().plusDays(1));
            when(bidSealRepository.save(any(BidSeal.class))).thenAnswer(invocation -> {
                BidSeal saved = invocation.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            BidSealDTO result = bidSealingService.sealBid(BID_ID, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getBidId()).isEqualTo(BID_ID);
            assertThat(result.getTenderId()).isEqualTo(TENDER_ID);
            assertThat(result.getStatus()).isEqualTo(SealStatus.SEALED);
            assertThat(result.getSealedBy()).isEqualTo(USER_ID);
            assertThat(result.getContentHash()).isNotBlank();
            assertThat(result.getEncryptionAlgorithm()).isEqualTo("AES/GCM/NoPadding");
            assertThat(result.getIntegrityVerified()).isTrue();

            ArgumentCaptor<BidSeal> captor = ArgumentCaptor.forClass(BidSeal.class);
            verify(bidSealRepository).save(captor.capture());
            BidSeal savedSeal = captor.getValue();
            assertThat(savedSeal.getContentHash()).isNotBlank();
            assertThat(savedSeal.getEncryptedContent()).isNotBlank();
        }

        @Test
        @DisplayName("should throw when bid is already sealed")
        void sealBid_alreadySealed_throwsIllegalState() {
            when(bidSealRepository.existsByBidId(BID_ID)).thenReturn(true);

            assertThatThrownBy(() -> bidSealingService.sealBid(BID_ID, USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Bid is already sealed");

            verify(bidRepository, never()).findById(anyLong());
            verify(bidSealRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when bid is not in SUBMITTED status")
        void sealBid_notSubmitted_throwsIllegalState() {
            sampleBid.setStatus(BidStatus.DRAFT);
            when(bidSealRepository.existsByBidId(BID_ID)).thenReturn(false);
            when(bidRepository.findById(BID_ID)).thenReturn(Optional.of(sampleBid));

            assertThatThrownBy(() -> bidSealingService.sealBid(BID_ID, USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only submitted bids can be sealed");

            verify(bidSealRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when bid is not found")
        void sealBid_bidNotFound_throwsEntityNotFound() {
            when(bidSealRepository.existsByBidId(BID_ID)).thenReturn(false);
            when(bidRepository.findById(BID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bidSealingService.sealBid(BID_ID, USER_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Bid not found");
        }
    }

    @Nested
    @DisplayName("unsealBid")
    class UnsealBidTests {

        @Test
        @DisplayName("should unseal a sealed bid after deadline")
        void unsealBid_success() throws Exception {
            // Seal has scheduledUnsealTime in the past
            sampleSeal.setScheduledUnsealTime(LocalDateTime.now().minusMinutes(30));
            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(sampleSeal));
            when(bidRepository.findById(BID_ID)).thenReturn(Optional.of(sampleBid));
            when(objectMapper.writeValueAsString(any(Bid.class))).thenReturn("{\"id\":1}");
            when(bidSealRepository.save(any(BidSeal.class))).thenAnswer(inv -> inv.getArgument(0));

            BidSealDTO result = bidSealingService.unsealBid(BID_ID, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getUnsealedBy()).isEqualTo(USER_ID);
            assertThat(result.getUnsealedAt()).isNotNull();
            // Status is either UNSEALED or TAMPER_DETECTED depending on hash match
            assertThat(result.getStatus()).isIn(SealStatus.UNSEALED, SealStatus.TAMPER_DETECTED);

            verify(bidSealRepository).save(any(BidSeal.class));
        }

        @Test
        @DisplayName("should throw when no seal found for bid")
        void unsealBid_noSealFound_throwsEntityNotFound() {
            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bidSealingService.unsealBid(BID_ID, USER_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No seal found for bid");
        }

        @Test
        @DisplayName("should throw when bid is not in SEALED status")
        void unsealBid_notSealed_throwsIllegalState() {
            sampleSeal.setStatus(SealStatus.UNSEALED);
            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(sampleSeal));

            assertThatThrownBy(() -> bidSealingService.unsealBid(BID_ID, USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Bid is not in SEALED status");

            verify(bidSealRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when trying to unseal before scheduled time")
        void unsealBid_beforeDeadline_throwsIllegalState() {
            sampleSeal.setStatus(SealStatus.SEALED);
            sampleSeal.setScheduledUnsealTime(LocalDateTime.now().plusDays(1));
            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(sampleSeal));

            assertThatThrownBy(() -> bidSealingService.unsealBid(BID_ID, USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot unseal before scheduled time");

            verify(bidSealRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("verifyBidIntegrity")
    class VerifyBidIntegrityTests {

        @Test
        @DisplayName("should return true when content hash matches")
        void verifyBidIntegrity_hashMatches_returnsTrue() throws Exception {
            String bidJson = "{\"id\":1,\"tenderId\":100}";
            // Compute expected hash the same way the service does
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(bidJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String expectedHash = java.util.HexFormat.of().formatHex(hashBytes);

            sampleSeal.setContentHash(expectedHash);

            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(sampleSeal));
            when(bidRepository.findById(BID_ID)).thenReturn(Optional.of(sampleBid));
            when(objectMapper.writeValueAsString(sampleBid)).thenReturn(bidJson);

            boolean result = bidSealingService.verifyBidIntegrity(BID_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when content hash does not match")
        void verifyBidIntegrity_hashMismatch_returnsFalse() throws Exception {
            sampleSeal.setContentHash("stale-hash-that-wont-match");

            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(sampleSeal));
            when(bidRepository.findById(BID_ID)).thenReturn(Optional.of(sampleBid));
            when(objectMapper.writeValueAsString(sampleBid)).thenReturn("{\"id\":1}");

            boolean result = bidSealingService.verifyBidIntegrity(BID_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should throw when no seal exists for bid")
        void verifyBidIntegrity_noSeal_throwsEntityNotFound() {
            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bidSealingService.verifyBidIntegrity(BID_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No seal found for bid");
        }

        @Test
        @DisplayName("should return false when bid not found during verification")
        void verifyBidIntegrity_bidNotFound_returnsFalse() {
            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(sampleSeal));
            when(bidRepository.findById(BID_ID)).thenReturn(Optional.empty());

            boolean result = bidSealingService.verifyBidIntegrity(BID_ID);

            assertThat(result).isFalse();
        }
    }
}
