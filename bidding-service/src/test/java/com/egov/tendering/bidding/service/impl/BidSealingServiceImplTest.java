package com.egov.tendering.bidding.service.impl;

import com.egov.tendering.bidding.config.BidSealingProperties;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.KeyGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BidSealingServiceImpl}.
 *
 * <p>Uses real cryptographic operations (envelope encryption) and mocked repositories.
 * A fresh 256-bit master key is generated for each test run to keep tests hermetic.
 */
@ExtendWith(MockitoExtension.class)
class BidSealingServiceImplTest {

    @Mock private BidSealRepository bidSealRepository;
    @Mock private BidRepository bidRepository;
    @Mock private TenderWorkflowGuard tenderWorkflowGuard;

    private BidSealingServiceImpl bidSealingService;
    private ObjectMapper objectMapper;

    private static final Long BID_ID    = 1L;
    private static final Long TENDER_ID = 100L;
    private static final Long USER_ID   = 50L;

    private Bid sampleBid;

    @BeforeEach
    void setUp() throws Exception {
        // Generate a fresh test master key for each test
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        String masterKeyB64 = Base64.getEncoder().encodeToString(kg.generateKey().getEncoded());

        BidSealingProperties props = new BidSealingProperties();
        props.setMasterKeyBase64(masterKeyB64);
        // All other properties use their defaults (AES/GCM/NoPadding, SHA-256, etc.)

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        bidSealingService = new BidSealingServiceImpl(
                bidSealRepository, bidRepository, tenderWorkflowGuard, objectMapper, props);

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
    }

    // -----------------------------------------------------------------------
    // Helper: perform a real seal and capture the saved BidSeal
    // -----------------------------------------------------------------------

    private BidSeal performSeal(LocalDateTime unsealTime) throws Exception {
        when(bidSealRepository.existsByBidId(BID_ID)).thenReturn(false);
        when(bidRepository.findById(BID_ID)).thenReturn(Optional.of(sampleBid));
        when(tenderWorkflowGuard.getSubmissionDeadline(TENDER_ID)).thenReturn(unsealTime);
        when(bidSealRepository.save(any(BidSeal.class))).thenAnswer(inv -> {
            BidSeal s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        bidSealingService.sealBid(BID_ID, USER_ID);

        ArgumentCaptor<BidSeal> captor = ArgumentCaptor.forClass(BidSeal.class);
        verify(bidSealRepository).save(captor.capture());
        // Reset mocks so subsequent calls in the same test are fresh
        reset(bidSealRepository, bidRepository, tenderWorkflowGuard);
        return captor.getValue();
    }

    // -----------------------------------------------------------------------
    // sealBid
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("sealBid")
    class SealBidTests {

        @Test
        @DisplayName("seals a submitted bid and stores wrapped key beside ciphertext")
        void sealBid_success() throws Exception {
            when(bidSealRepository.existsByBidId(BID_ID)).thenReturn(false);
            when(bidRepository.findById(BID_ID)).thenReturn(Optional.of(sampleBid));
            when(tenderWorkflowGuard.getSubmissionDeadline(TENDER_ID))
                    .thenReturn(LocalDateTime.now().plusDays(1));
            when(bidSealRepository.save(any(BidSeal.class))).thenAnswer(inv -> {
                BidSeal s = inv.getArgument(0);
                s.setId(10L);
                return s;
            });

            BidSealDTO result = bidSealingService.sealBid(BID_ID, USER_ID);

            assertThat(result.getBidId()).isEqualTo(BID_ID);
            assertThat(result.getStatus()).isEqualTo(SealStatus.SEALED);
            assertThat(result.getContentHash()).isNotBlank();
            assertThat(result.getEncryptionAlgorithm()).isEqualTo("AES/GCM/NoPadding");

            ArgumentCaptor<BidSeal> captor = ArgumentCaptor.forClass(BidSeal.class);
            verify(bidSealRepository).save(captor.capture());
            BidSeal saved = captor.getValue();

            assertThat(saved.getEncryptedContent()).isNotBlank();
            assertThat(saved.getSealKeyReference()).isNotBlank();
            // The wrapped key must NOT be the raw CEK bytes — it must be longer
            // (IV + ciphertext of the wrapped key) and not equal to the CEK length
            byte[] wrappedBytes = Base64.getDecoder().decode(saved.getSealKeyReference());
            assertThat(wrappedBytes.length).isGreaterThan(32); // 12 IV + 32 key + 16 GCM tag = 60
        }

        @Test
        @DisplayName("throws when bid is already sealed")
        void sealBid_alreadySealed() {
            when(bidSealRepository.existsByBidId(BID_ID)).thenReturn(true);

            assertThatThrownBy(() -> bidSealingService.sealBid(BID_ID, USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Bid is already sealed");

            verify(bidRepository, never()).findById(anyLong());
            verify(bidSealRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when bid is not in SUBMITTED status")
        void sealBid_notSubmitted() {
            sampleBid.setStatus(BidStatus.DRAFT);
            when(bidSealRepository.existsByBidId(BID_ID)).thenReturn(false);
            when(bidRepository.findById(BID_ID)).thenReturn(Optional.of(sampleBid));

            assertThatThrownBy(() -> bidSealingService.sealBid(BID_ID, USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only submitted bids can be sealed");

            verify(bidSealRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when bid is not found")
        void sealBid_bidNotFound() {
            when(bidSealRepository.existsByBidId(BID_ID)).thenReturn(false);
            when(bidRepository.findById(BID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bidSealingService.sealBid(BID_ID, USER_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Bid not found");
        }
    }

    // -----------------------------------------------------------------------
    // unsealBid
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("unsealBid")
    class UnsealBidTests {

        @Test
        @DisplayName("unseals a sealed bid after deadline with verified integrity")
        void unsealBid_success() throws Exception {
            BidSeal seal = performSeal(LocalDateTime.now().minusMinutes(30));
            seal.setStatus(SealStatus.SEALED);

            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(seal));
            when(bidSealRepository.save(any(BidSeal.class))).thenAnswer(inv -> inv.getArgument(0));

            BidSealDTO result = bidSealingService.unsealBid(BID_ID, USER_ID);

            assertThat(result.getStatus()).isEqualTo(SealStatus.UNSEALED);
            assertThat(result.getIntegrityVerified()).isTrue();
            assertThat(result.getUnsealedBy()).isEqualTo(USER_ID);
            assertThat(result.getUnsealedAt()).isNotNull();
        }

        @Test
        @DisplayName("detects tampered encrypted content and sets TAMPER_DETECTED")
        void unsealBid_tamperedContent_tamperDetected() throws Exception {
            BidSeal seal = performSeal(LocalDateTime.now().minusMinutes(30));
            seal.setStatus(SealStatus.SEALED);
            // Corrupt the encrypted content — integrity check must fail
            seal.setEncryptedContent(Base64.getEncoder().encodeToString("corrupted-garbage".getBytes()));

            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(seal));
            when(bidSealRepository.save(any(BidSeal.class))).thenAnswer(inv -> inv.getArgument(0));

            BidSealDTO result = bidSealingService.unsealBid(BID_ID, USER_ID);

            assertThat(result.getStatus()).isEqualTo(SealStatus.TAMPER_DETECTED);
            assertThat(result.getIntegrityVerified()).isFalse();
        }

        @Test
        @DisplayName("mutating Bid ORM fields after sealing does NOT cause false TAMPER_DETECTED")
        void unsealBid_ormFieldChangedAfterSeal_integrityStillValid() throws Exception {
            BidSeal seal = performSeal(LocalDateTime.now().minusMinutes(30));
            seal.setStatus(SealStatus.SEALED);

            // Simulate ORM touching updatedAt after sealing — this must not affect integrity
            sampleBid.setUpdatedAt(LocalDateTime.now());

            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(seal));
            when(bidSealRepository.save(any(BidSeal.class))).thenAnswer(inv -> inv.getArgument(0));

            BidSealDTO result = bidSealingService.unsealBid(BID_ID, USER_ID);

            // Integrity is based on the sealed artifact, not the live entity — must pass
            assertThat(result.getStatus()).isEqualTo(SealStatus.UNSEALED);
            assertThat(result.getIntegrityVerified()).isTrue();
        }

        @Test
        @DisplayName("throws when no seal found for bid")
        void unsealBid_noSealFound() {
            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bidSealingService.unsealBid(BID_ID, USER_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No seal found for bid");
        }

        @Test
        @DisplayName("throws when bid is not in SEALED status")
        void unsealBid_notSealed() {
            BidSeal seal = BidSeal.builder()
                    .id(10L).bidId(BID_ID).status(SealStatus.UNSEALED)
                    .scheduledUnsealTime(LocalDateTime.now().minusMinutes(30))
                    .build();
            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(seal));

            assertThatThrownBy(() -> bidSealingService.unsealBid(BID_ID, USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Bid is not in SEALED status");

            verify(bidSealRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when trying to unseal before scheduled time (deadline enforcement)")
        void unsealBid_beforeDeadline() throws Exception {
            BidSeal seal = performSeal(LocalDateTime.now().plusDays(1));
            seal.setStatus(SealStatus.SEALED);

            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(seal));

            assertThatThrownBy(() -> bidSealingService.unsealBid(BID_ID, USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot unseal before scheduled time");

            verify(bidSealRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // verifyBidIntegrity
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("verifyBidIntegrity")
    class VerifyBidIntegrityTests {

        @Test
        @DisplayName("returns true for an intact sealed bid")
        void verifyBidIntegrity_intact_returnsTrue() throws Exception {
            BidSeal seal = performSeal(LocalDateTime.now().plusDays(1));

            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(seal));

            assertThat(bidSealingService.verifyBidIntegrity(BID_ID)).isTrue();
        }

        @Test
        @DisplayName("returns false when encrypted content is corrupted")
        void verifyBidIntegrity_corruptedContent_returnsFalse() throws Exception {
            BidSeal seal = performSeal(LocalDateTime.now().plusDays(1));
            seal.setEncryptedContent(Base64.getEncoder().encodeToString("corrupted".getBytes()));

            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(seal));

            assertThat(bidSealingService.verifyBidIntegrity(BID_ID)).isFalse();
        }

        @Test
        @DisplayName("returns false when the stored hash is altered")
        void verifyBidIntegrity_alteredHash_returnsFalse() throws Exception {
            BidSeal seal = performSeal(LocalDateTime.now().plusDays(1));
            seal.setContentHash("000000000000000000000000000000000000000000000000000000000000dead");

            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(seal));

            assertThat(bidSealingService.verifyBidIntegrity(BID_ID)).isFalse();
        }

        @Test
        @DisplayName("returns false when the wrapped key is corrupted")
        void verifyBidIntegrity_corruptedKey_returnsFalse() throws Exception {
            BidSeal seal = performSeal(LocalDateTime.now().plusDays(1));
            seal.setSealKeyReference(Base64.getEncoder().encodeToString("not-a-valid-wrapped-key".getBytes()));

            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.of(seal));

            assertThat(bidSealingService.verifyBidIntegrity(BID_ID)).isFalse();
        }

        @Test
        @DisplayName("throws when no seal exists for bid")
        void verifyBidIntegrity_noSeal_throwsEntityNotFound() {
            when(bidSealRepository.findByBidId(BID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bidSealingService.verifyBidIntegrity(BID_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No seal found for bid");
        }
    }

    // -----------------------------------------------------------------------
    // processScheduledUnseals
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("processScheduledUnseals")
    class ScheduledUnsealTests {

        @Test
        @DisplayName("auto-unseals bids whose scheduled time has passed")
        void processScheduledUnseals_unsealsReadyBids() throws Exception {
            BidSeal seal = performSeal(LocalDateTime.now().minusMinutes(5));
            seal.setStatus(SealStatus.SEALED);

            when(bidSealRepository.findSealedBidsReadyForOpening(eq(SealStatus.SEALED), any()))
                    .thenReturn(List.of(seal));
            when(bidSealRepository.save(any(BidSeal.class))).thenAnswer(inv -> inv.getArgument(0));

            bidSealingService.processScheduledUnseals();

            ArgumentCaptor<BidSeal> captor = ArgumentCaptor.forClass(BidSeal.class);
            verify(bidSealRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SealStatus.UNSEALED);
            assertThat(captor.getValue().getIntegrityVerified()).isTrue();
        }

        @Test
        @DisplayName("sets TAMPER_DETECTED for a corrupted bid during scheduled unseal")
        void processScheduledUnseals_corruptedBid_tamperDetected() throws Exception {
            BidSeal seal = performSeal(LocalDateTime.now().minusMinutes(5));
            seal.setStatus(SealStatus.SEALED);
            seal.setEncryptedContent(Base64.getEncoder().encodeToString("corrupted".getBytes()));

            when(bidSealRepository.findSealedBidsReadyForOpening(eq(SealStatus.SEALED), any()))
                    .thenReturn(List.of(seal));
            when(bidSealRepository.save(any(BidSeal.class))).thenAnswer(inv -> inv.getArgument(0));

            bidSealingService.processScheduledUnseals();

            ArgumentCaptor<BidSeal> captor = ArgumentCaptor.forClass(BidSeal.class);
            verify(bidSealRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SealStatus.TAMPER_DETECTED);
        }

        @Test
        @DisplayName("does nothing when no bids are ready for unsealing")
        void processScheduledUnseals_noBids_noSaves() {
            when(bidSealRepository.findSealedBidsReadyForOpening(any(), any()))
                    .thenReturn(List.of());

            bidSealingService.processScheduledUnseals();

            verify(bidSealRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // Constructor validation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("startup validation")
    class StartupValidationTests {

        @Test
        @DisplayName("throws on null master key")
        void constructor_nullMasterKey_throws() {
            BidSealingProperties props = new BidSealingProperties();
            props.setMasterKeyBase64(null);

            assertThatThrownBy(() -> new BidSealingServiceImpl(
                    bidSealRepository, bidRepository, tenderWorkflowGuard, objectMapper, props))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("master key");
        }

        @Test
        @DisplayName("throws when master key is not 256 bits")
        void constructor_wrongKeyLength_throws() {
            // 16-byte (128-bit) key — invalid, must be 32 bytes
            BidSealingProperties props = new BidSealingProperties();
            props.setMasterKeyBase64(Base64.getEncoder().encodeToString(new byte[16]));

            assertThatThrownBy(() -> new BidSealingServiceImpl(
                    bidSealRepository, bidRepository, tenderWorkflowGuard, objectMapper, props))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("256 bits");
        }
    }
}
