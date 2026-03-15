package com.egov.tendering.bidding.service.impl;

import com.egov.tendering.bidding.dal.dto.AntiCollusionReport;
import com.egov.tendering.bidding.dal.model.Bid;
import com.egov.tendering.bidding.dal.model.BidStatus;
import com.egov.tendering.bidding.dal.model.BidSubmissionMetadata;
import com.egov.tendering.bidding.dal.repository.BidRepository;
import com.egov.tendering.bidding.dal.repository.BidSubmissionMetadataRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AntiCollusionServiceImplTest {

    @Mock
    private BidSubmissionMetadataRepository metadataRepository;

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private AntiCollusionServiceImpl antiCollusionService;

    private static final Long BID_ID = 1L;
    private static final Long TENDER_ID = 100L;
    private static final Long TENDERER_ID = 200L;

    @Nested
    @DisplayName("recordSubmissionMetadata")
    class RecordSubmissionMetadataTests {

        @Test
        @DisplayName("should record metadata with no flags when IP is unique")
        void recordMetadata_cleanSubmission() {
            when(metadataRepository.save(any(BidSubmissionMetadata.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(metadataRepository.findSameIpDifferentTenderer(eq(TENDER_ID), eq("192.168.1.1"), eq(TENDERER_ID)))
                    .thenReturn(Collections.emptyList());
            when(metadataRepository.findSameDeviceDifferentTenderer(eq(TENDER_ID), eq("fp-abc"), eq(TENDERER_ID)))
                    .thenReturn(Collections.emptyList());

            antiCollusionService.recordSubmissionMetadata(
                    BID_ID, TENDER_ID, TENDERER_ID,
                    "192.168.1.1", "Mozilla/5.0", "fp-abc", "session-1");

            ArgumentCaptor<BidSubmissionMetadata> captor = ArgumentCaptor.forClass(BidSubmissionMetadata.class);
            verify(metadataRepository).save(captor.capture());
            BidSubmissionMetadata saved = captor.getValue();
            assertThat(saved.getBidId()).isEqualTo(BID_ID);
            assertThat(saved.getTenderId()).isEqualTo(TENDER_ID);
            assertThat(saved.getTendererId()).isEqualTo(TENDERER_ID);
            assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
            assertThat(saved.getDeviceFingerprint()).isEqualTo("fp-abc");
            assertThat(saved.getSessionId()).isEqualTo("session-1");
        }

        @Test
        @DisplayName("should flag metadata when same IP from different tenderer is detected")
        void recordMetadata_sameIpFlagged() {
            BidSubmissionMetadata existing = BidSubmissionMetadata.builder()
                    .bidId(2L)
                    .tenderId(TENDER_ID)
                    .tendererId(300L)
                    .ipAddress("192.168.1.1")
                    .build();

            when(metadataRepository.save(any(BidSubmissionMetadata.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(metadataRepository.findSameIpDifferentTenderer(eq(TENDER_ID), eq("192.168.1.1"), eq(TENDERER_ID)))
                    .thenReturn(List.of(existing));

            antiCollusionService.recordSubmissionMetadata(
                    BID_ID, TENDER_ID, TENDERER_ID,
                    "192.168.1.1", "Mozilla/5.0", null, "session-1");

            // save is called twice: once for initial save, once after flagging
            verify(metadataRepository, atLeast(2)).save(any(BidSubmissionMetadata.class));
        }

        @Test
        @DisplayName("should flag metadata when same device fingerprint from different tenderer is detected")
        void recordMetadata_sameDeviceFlagged() {
            BidSubmissionMetadata existing = BidSubmissionMetadata.builder()
                    .bidId(3L)
                    .tenderId(TENDER_ID)
                    .tendererId(400L)
                    .deviceFingerprint("fp-shared")
                    .build();

            when(metadataRepository.save(any(BidSubmissionMetadata.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(metadataRepository.findSameIpDifferentTenderer(eq(TENDER_ID), eq("10.0.0.1"), eq(TENDERER_ID)))
                    .thenReturn(Collections.emptyList());
            when(metadataRepository.findSameDeviceDifferentTenderer(eq(TENDER_ID), eq("fp-shared"), eq(TENDERER_ID)))
                    .thenReturn(List.of(existing));

            antiCollusionService.recordSubmissionMetadata(
                    BID_ID, TENDER_ID, TENDERER_ID,
                    "10.0.0.1", "Chrome/120", "fp-shared", "session-2");

            verify(metadataRepository, atLeast(2)).save(any(BidSubmissionMetadata.class));
        }
    }

    @Nested
    @DisplayName("analyzeForCollusion")
    class AnalyzeForCollusionTests {

        @Test
        @DisplayName("should detect same IP address collusion flag")
        void analyze_sameIpAddress() {
            BidSubmissionMetadata m1 = BidSubmissionMetadata.builder()
                    .bidId(1L).tenderId(TENDER_ID).tendererId(200L)
                    .ipAddress("192.168.1.1").submissionTime(LocalDateTime.now().minusHours(2))
                    .build();
            BidSubmissionMetadata m2 = BidSubmissionMetadata.builder()
                    .bidId(2L).tenderId(TENDER_ID).tendererId(300L)
                    .ipAddress("192.168.1.1").submissionTime(LocalDateTime.now().minusHours(1))
                    .build();

            when(metadataRepository.findByTenderId(TENDER_ID)).thenReturn(List.of(m1, m2));
            when(bidRepository.findByTenderIdAndStatusNot(TENDER_ID, BidStatus.CANCELLED))
                    .thenReturn(Collections.emptyList());

            AntiCollusionReport report = antiCollusionService.analyzeForCollusion(TENDER_ID);

            assertThat(report.getTenderId()).isEqualTo(TENDER_ID);
            assertThat(report.isCollusionSuspected()).isTrue();
            assertThat(report.getFlags()).isNotEmpty();
            assertThat(report.getFlags())
                    .anyMatch(f -> "SAME_IP_ADDRESS".equals(f.getType()));
        }

        @Test
        @DisplayName("should detect same device fingerprint collusion flag")
        void analyze_sameDevice() {
            BidSubmissionMetadata m1 = BidSubmissionMetadata.builder()
                    .bidId(1L).tenderId(TENDER_ID).tendererId(200L)
                    .deviceFingerprint("device-xyz").submissionTime(LocalDateTime.now().minusHours(5))
                    .build();
            BidSubmissionMetadata m2 = BidSubmissionMetadata.builder()
                    .bidId(2L).tenderId(TENDER_ID).tendererId(300L)
                    .deviceFingerprint("device-xyz").submissionTime(LocalDateTime.now().minusHours(3))
                    .build();

            when(metadataRepository.findByTenderId(TENDER_ID)).thenReturn(List.of(m1, m2));
            when(bidRepository.findByTenderIdAndStatusNot(TENDER_ID, BidStatus.CANCELLED))
                    .thenReturn(Collections.emptyList());

            AntiCollusionReport report = antiCollusionService.analyzeForCollusion(TENDER_ID);

            assertThat(report.isCollusionSuspected()).isTrue();
            assertThat(report.getFlags())
                    .anyMatch(f -> "SAME_DEVICE".equals(f.getType()));
        }

        @Test
        @DisplayName("should detect suspicious pricing patterns")
        void analyze_pricingPatterns() {
            // Two bids with prices within 2% of each other
            Bid bid1 = Bid.builder()
                    .id(1L).tenderId(TENDER_ID).tendererId(200L)
                    .totalPrice(new BigDecimal("100000.00")).status(BidStatus.SUBMITTED).build();
            Bid bid2 = Bid.builder()
                    .id(2L).tenderId(TENDER_ID).tendererId(300L)
                    .totalPrice(new BigDecimal("100500.00")).status(BidStatus.SUBMITTED).build();

            when(metadataRepository.findByTenderId(TENDER_ID)).thenReturn(Collections.emptyList());
            when(bidRepository.findByTenderIdAndStatusNot(TENDER_ID, BidStatus.CANCELLED))
                    .thenReturn(List.of(bid1, bid2));

            AntiCollusionReport report = antiCollusionService.analyzeForCollusion(TENDER_ID);

            assertThat(report.isCollusionSuspected()).isTrue();
            assertThat(report.getFlags())
                    .anyMatch(f -> "SUSPICIOUS_PRICING".equals(f.getType()));
        }

        @Test
        @DisplayName("should detect timing anomalies when bids submitted within 60 seconds")
        void analyze_timingAnomalies() {
            LocalDateTime baseTime = LocalDateTime.of(2026, 1, 15, 10, 0, 0);

            BidSubmissionMetadata m1 = BidSubmissionMetadata.builder()
                    .bidId(1L).tenderId(TENDER_ID).tendererId(200L)
                    .submissionTime(baseTime)
                    .build();
            BidSubmissionMetadata m2 = BidSubmissionMetadata.builder()
                    .bidId(2L).tenderId(TENDER_ID).tendererId(300L)
                    .submissionTime(baseTime.plusSeconds(30))
                    .build();

            when(metadataRepository.findByTenderId(TENDER_ID)).thenReturn(List.of(m1, m2));
            when(bidRepository.findByTenderIdAndStatusNot(TENDER_ID, BidStatus.CANCELLED))
                    .thenReturn(Collections.emptyList());

            AntiCollusionReport report = antiCollusionService.analyzeForCollusion(TENDER_ID);

            assertThat(report.isCollusionSuspected()).isTrue();
            assertThat(report.getFlags())
                    .anyMatch(f -> "TIMING_ANOMALY".equals(f.getType()));
        }

        @Test
        @DisplayName("should report no collusion when all checks are clean")
        void analyze_noCollusion() {
            BidSubmissionMetadata m1 = BidSubmissionMetadata.builder()
                    .bidId(1L).tenderId(TENDER_ID).tendererId(200L)
                    .ipAddress("10.0.0.1").deviceFingerprint("dev-1")
                    .submissionTime(LocalDateTime.now().minusHours(5))
                    .build();
            BidSubmissionMetadata m2 = BidSubmissionMetadata.builder()
                    .bidId(2L).tenderId(TENDER_ID).tendererId(300L)
                    .ipAddress("10.0.0.2").deviceFingerprint("dev-2")
                    .submissionTime(LocalDateTime.now().minusHours(3))
                    .build();

            Bid bid1 = Bid.builder()
                    .id(1L).tenderId(TENDER_ID).tendererId(200L)
                    .totalPrice(new BigDecimal("100000.00")).status(BidStatus.SUBMITTED).build();
            Bid bid2 = Bid.builder()
                    .id(2L).tenderId(TENDER_ID).tendererId(300L)
                    .totalPrice(new BigDecimal("150000.00")).status(BidStatus.SUBMITTED).build();

            when(metadataRepository.findByTenderId(TENDER_ID)).thenReturn(List.of(m1, m2));
            when(bidRepository.findByTenderIdAndStatusNot(TENDER_ID, BidStatus.CANCELLED))
                    .thenReturn(List.of(bid1, bid2));

            AntiCollusionReport report = antiCollusionService.analyzeForCollusion(TENDER_ID);

            assertThat(report.isCollusionSuspected()).isFalse();
            assertThat(report.getFlags()).isEmpty();
            assertThat(report.getTotalBids()).isEqualTo(2);
            assertThat(report.getFlaggedBids()).isZero();
        }

        @Test
        @DisplayName("should correctly count flagged bids across multiple flags")
        void analyze_flaggedBidCount() {
            // Same IP + same device from same two tenderers = both flags reference same bid IDs
            BidSubmissionMetadata m1 = BidSubmissionMetadata.builder()
                    .bidId(1L).tenderId(TENDER_ID).tendererId(200L)
                    .ipAddress("192.168.1.1").deviceFingerprint("device-shared")
                    .submissionTime(LocalDateTime.now().minusHours(5))
                    .build();
            BidSubmissionMetadata m2 = BidSubmissionMetadata.builder()
                    .bidId(2L).tenderId(TENDER_ID).tendererId(300L)
                    .ipAddress("192.168.1.1").deviceFingerprint("device-shared")
                    .submissionTime(LocalDateTime.now().minusHours(3))
                    .build();

            when(metadataRepository.findByTenderId(TENDER_ID)).thenReturn(List.of(m1, m2));
            when(bidRepository.findByTenderIdAndStatusNot(TENDER_ID, BidStatus.CANCELLED))
                    .thenReturn(Collections.emptyList());

            AntiCollusionReport report = antiCollusionService.analyzeForCollusion(TENDER_ID);

            assertThat(report.isCollusionSuspected()).isTrue();
            // flaggedBids counts distinct bid IDs across all flags
            assertThat(report.getFlaggedBids()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("flagBid")
    class FlagBidTests {

        @Test
        @DisplayName("should flag a bid with the given reason")
        void flagBid_success() {
            BidSubmissionMetadata metadata = BidSubmissionMetadata.builder()
                    .id(5L)
                    .bidId(BID_ID)
                    .tenderId(TENDER_ID)
                    .tendererId(TENDERER_ID)
                    .flagged(false)
                    .build();

            when(metadataRepository.findByBidId(BID_ID)).thenReturn(Optional.of(metadata));
            when(metadataRepository.save(any(BidSubmissionMetadata.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            antiCollusionService.flagBid(BID_ID, "Suspicious IP match");

            ArgumentCaptor<BidSubmissionMetadata> captor = ArgumentCaptor.forClass(BidSubmissionMetadata.class);
            verify(metadataRepository).save(captor.capture());
            BidSubmissionMetadata saved = captor.getValue();
            assertThat(saved.getFlagged()).isTrue();
            assertThat(saved.getFlagReason()).isEqualTo("Suspicious IP match");
        }

        @Test
        @DisplayName("should throw when no metadata found for bid")
        void flagBid_noMetadata_throwsEntityNotFound() {
            when(metadataRepository.findByBidId(BID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> antiCollusionService.flagBid(BID_ID, "test reason"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No metadata found for bid");

            verify(metadataRepository, never()).save(any());
        }
    }
}
