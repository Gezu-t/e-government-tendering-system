package com.egov.tendering.bidding.service.impl;

import com.egov.tendering.bidding.dal.dto.AntiCollusionReport;
import com.egov.tendering.bidding.dal.dto.AntiCollusionReport.CollusionFlag;
import com.egov.tendering.bidding.dal.model.Bid;
import com.egov.tendering.bidding.dal.model.BidStatus;
import com.egov.tendering.bidding.dal.model.BidSubmissionMetadata;
import com.egov.tendering.bidding.dal.repository.BidRepository;
import com.egov.tendering.bidding.dal.repository.BidSubmissionMetadataRepository;
import com.egov.tendering.bidding.service.AntiCollusionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AntiCollusionServiceImpl implements AntiCollusionService {

    private static final double PRICE_SIMILARITY_THRESHOLD = 0.02; // 2% threshold
    private static final long TIMING_ANOMALY_SECONDS = 60; // Within 60 seconds

    private final BidSubmissionMetadataRepository metadataRepository;
    private final BidRepository bidRepository;

    @Override
    @Transactional
    public void recordSubmissionMetadata(Long bidId, Long tenderId, Long tendererId,
                                          String ipAddress, String userAgent,
                                          String deviceFingerprint, String sessionId) {
        log.info("Recording submission metadata for bid: {} in tender: {}", bidId, tenderId);

        BidSubmissionMetadata metadata = BidSubmissionMetadata.builder()
                .bidId(bidId)
                .tenderId(tenderId)
                .tendererId(tendererId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceFingerprint(deviceFingerprint)
                .sessionId(sessionId)
                .submissionTime(java.time.LocalDateTime.now())
                .build();

        metadataRepository.save(metadata);

        // Immediately check for obvious collusion indicators
        performQuickCheck(metadata);
    }

    @Override
    public AntiCollusionReport analyzeForCollusion(Long tenderId) {
        log.info("Analyzing tender {} for potential collusion", tenderId);

        List<BidSubmissionMetadata> allMetadata = metadataRepository.findByTenderId(tenderId);
        List<Bid> allBids = bidRepository.findByTenderIdAndStatusNot(tenderId, BidStatus.CANCELLED);

        List<CollusionFlag> flags = new ArrayList<>();

        // Check 1: Same IP address from different tenderers
        flags.addAll(checkSameIpAddress(allMetadata));

        // Check 2: Same device fingerprint from different tenderers
        flags.addAll(checkSameDevice(allMetadata));

        // Check 3: Suspicious pricing patterns (bid prices too close together)
        flags.addAll(checkPricingPatterns(allBids));

        // Check 4: Submission timing anomalies
        flags.addAll(checkTimingAnomalies(allMetadata));

        int flaggedCount = (int) flags.stream()
                .flatMap(f -> f.getInvolvedBidIds().stream())
                .distinct()
                .count();

        AntiCollusionReport report = AntiCollusionReport.builder()
                .tenderId(tenderId)
                .totalBids(allBids.size())
                .flaggedBids(flaggedCount)
                .flags(flags)
                .collusionSuspected(!flags.isEmpty())
                .build();

        log.info("Collusion analysis for tender {}: {} flags found, collusion suspected: {}",
                tenderId, flags.size(), report.isCollusionSuspected());

        return report;
    }

    @Override
    @Transactional
    public void flagBid(Long bidId, String reason) {
        BidSubmissionMetadata metadata = metadataRepository.findByBidId(bidId)
                .orElseThrow(() -> new EntityNotFoundException("No metadata found for bid: " + bidId));

        metadata.setFlagged(true);
        metadata.setFlagReason(reason);
        metadataRepository.save(metadata);

        log.warn("Bid {} flagged for potential collusion: {}", bidId, reason);
    }

    private void performQuickCheck(BidSubmissionMetadata metadata) {
        if (metadata.getIpAddress() != null) {
            List<BidSubmissionMetadata> sameIp = metadataRepository
                    .findSameIpDifferentTenderer(metadata.getTenderId(),
                            metadata.getIpAddress(), metadata.getTendererId());
            if (!sameIp.isEmpty()) {
                metadata.setFlagged(true);
                metadata.setFlagReason("Same IP address detected from different tenderer(s)");
                metadataRepository.save(metadata);
                log.warn("COLLUSION ALERT: Bid {} shares IP {} with {} other bid(s) from different tenderers",
                        metadata.getBidId(), metadata.getIpAddress(), sameIp.size());
            }
        }

        if (metadata.getDeviceFingerprint() != null) {
            List<BidSubmissionMetadata> sameDevice = metadataRepository
                    .findSameDeviceDifferentTenderer(metadata.getTenderId(),
                            metadata.getDeviceFingerprint(), metadata.getTendererId());
            if (!sameDevice.isEmpty()) {
                metadata.setFlagged(true);
                metadata.setFlagReason("Same device fingerprint detected from different tenderer(s)");
                metadataRepository.save(metadata);
                log.warn("COLLUSION ALERT: Bid {} shares device with {} other bid(s) from different tenderers",
                        metadata.getBidId(), sameDevice.size());
            }
        }
    }

    private List<CollusionFlag> checkSameIpAddress(List<BidSubmissionMetadata> allMetadata) {
        List<CollusionFlag> flags = new ArrayList<>();

        Map<String, List<BidSubmissionMetadata>> byIp = allMetadata.stream()
                .filter(m -> m.getIpAddress() != null)
                .collect(Collectors.groupingBy(BidSubmissionMetadata::getIpAddress));

        for (Map.Entry<String, List<BidSubmissionMetadata>> entry : byIp.entrySet()) {
            List<BidSubmissionMetadata> group = entry.getValue();
            Set<Long> uniqueTenderers = group.stream()
                    .map(BidSubmissionMetadata::getTendererId)
                    .collect(Collectors.toSet());

            if (uniqueTenderers.size() > 1) {
                flags.add(CollusionFlag.builder()
                        .type("SAME_IP_ADDRESS")
                        .description("Multiple tenderers submitted bids from IP: " + entry.getKey())
                        .involvedBidIds(group.stream().map(BidSubmissionMetadata::getBidId).collect(Collectors.toList()))
                        .involvedTendererIds(new ArrayList<>(uniqueTenderers))
                        .severity("HIGH")
                        .build());
            }
        }

        return flags;
    }

    private List<CollusionFlag> checkSameDevice(List<BidSubmissionMetadata> allMetadata) {
        List<CollusionFlag> flags = new ArrayList<>();

        Map<String, List<BidSubmissionMetadata>> byDevice = allMetadata.stream()
                .filter(m -> m.getDeviceFingerprint() != null)
                .collect(Collectors.groupingBy(BidSubmissionMetadata::getDeviceFingerprint));

        for (Map.Entry<String, List<BidSubmissionMetadata>> entry : byDevice.entrySet()) {
            List<BidSubmissionMetadata> group = entry.getValue();
            Set<Long> uniqueTenderers = group.stream()
                    .map(BidSubmissionMetadata::getTendererId)
                    .collect(Collectors.toSet());

            if (uniqueTenderers.size() > 1) {
                flags.add(CollusionFlag.builder()
                        .type("SAME_DEVICE")
                        .description("Multiple tenderers submitted bids from the same device")
                        .involvedBidIds(group.stream().map(BidSubmissionMetadata::getBidId).collect(Collectors.toList()))
                        .involvedTendererIds(new ArrayList<>(uniqueTenderers))
                        .severity("HIGH")
                        .build());
            }
        }

        return flags;
    }

    private List<CollusionFlag> checkPricingPatterns(List<Bid> allBids) {
        List<CollusionFlag> flags = new ArrayList<>();

        if (allBids.size() < 2) return flags;

        List<Bid> sortedBids = allBids.stream()
                .filter(b -> b.getTotalPrice() != null && b.getTotalPrice().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Bid::getTotalPrice))
                .toList();

        for (int i = 0; i < sortedBids.size() - 1; i++) {
            Bid bid1 = sortedBids.get(i);
            Bid bid2 = sortedBids.get(i + 1);

            BigDecimal priceDiff = bid2.getTotalPrice().subtract(bid1.getTotalPrice()).abs();
            BigDecimal avgPrice = bid1.getTotalPrice().add(bid2.getTotalPrice())
                    .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);

            if (avgPrice.compareTo(BigDecimal.ZERO) > 0) {
                double percentDiff = priceDiff.divide(avgPrice, 6, RoundingMode.HALF_UP).doubleValue();

                if (percentDiff < PRICE_SIMILARITY_THRESHOLD && !bid1.getTendererId().equals(bid2.getTendererId())) {
                    flags.add(CollusionFlag.builder()
                            .type("SUSPICIOUS_PRICING")
                            .description(String.format("Bid prices within %.1f%% of each other (%.2f vs %.2f)",
                                    percentDiff * 100, bid1.getTotalPrice(), bid2.getTotalPrice()))
                            .involvedBidIds(List.of(bid1.getId(), bid2.getId()))
                            .involvedTendererIds(List.of(bid1.getTendererId(), bid2.getTendererId()))
                            .severity("MEDIUM")
                            .build());
                }
            }
        }

        return flags;
    }

    private List<CollusionFlag> checkTimingAnomalies(List<BidSubmissionMetadata> allMetadata) {
        List<CollusionFlag> flags = new ArrayList<>();

        List<BidSubmissionMetadata> sorted = allMetadata.stream()
                .filter(m -> m.getSubmissionTime() != null)
                .sorted(Comparator.comparing(BidSubmissionMetadata::getSubmissionTime))
                .toList();

        for (int i = 0; i < sorted.size() - 1; i++) {
            BidSubmissionMetadata m1 = sorted.get(i);
            BidSubmissionMetadata m2 = sorted.get(i + 1);

            if (!m1.getTendererId().equals(m2.getTendererId())) {
                long secondsBetween = Duration.between(m1.getSubmissionTime(), m2.getSubmissionTime()).getSeconds();

                if (Math.abs(secondsBetween) <= TIMING_ANOMALY_SECONDS) {
                    flags.add(CollusionFlag.builder()
                            .type("TIMING_ANOMALY")
                            .description(String.format("Bids submitted within %d seconds of each other", secondsBetween))
                            .involvedBidIds(List.of(m1.getBidId(), m2.getBidId()))
                            .involvedTendererIds(List.of(m1.getTendererId(), m2.getTendererId()))
                            .severity("LOW")
                            .build());
                }
            }
        }

        return flags;
    }
}
