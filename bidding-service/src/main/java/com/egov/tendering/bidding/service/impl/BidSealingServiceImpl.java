package com.egov.tendering.bidding.service.impl;

import com.egov.tendering.bidding.dal.dto.BidSealDTO;
import com.egov.tendering.bidding.dal.model.*;
import com.egov.tendering.bidding.dal.repository.BidRepository;
import com.egov.tendering.bidding.dal.repository.BidSealRepository;
import com.egov.tendering.bidding.service.BidSealingService;
import com.egov.tendering.bidding.service.TenderWorkflowGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidSealingServiceImpl implements BidSealingService {

    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final BidSealRepository bidSealRepository;
    private final BidRepository bidRepository;
    private final TenderWorkflowGuard tenderWorkflowGuard;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public BidSealDTO sealBid(Long bidId, Long userId) {
        log.info("Sealing bid: {} by user: {}", bidId, userId);

        if (bidSealRepository.existsByBidId(bidId)) {
            throw new IllegalStateException("Bid is already sealed");
        }

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new EntityNotFoundException("Bid not found: " + bidId));

        if (bid.getStatus() != BidStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted bids can be sealed");
        }

        try {
            String bidContent = objectMapper.writeValueAsString(bid);
            String contentHash = computeHash(bidContent);

            // Generate encryption key and encrypt content
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            String keyReference = Base64.getEncoder().encodeToString(secretKey.getEncoded());

            String encryptedContent = encrypt(bidContent, secretKey);

            // Determine unseal time from tender submission deadline
            // The bid remains sealed until the tender's submission deadline
            LocalDateTime scheduledUnsealTime = tenderWorkflowGuard.getSubmissionDeadline(bid.getTenderId());

            BidSeal seal = BidSeal.builder()
                    .bidId(bidId)
                    .tenderId(bid.getTenderId())
                    .contentHash(contentHash)
                    .encryptedContent(encryptedContent)
                    .encryptionAlgorithm(ENCRYPTION_ALGORITHM)
                    .sealKeyReference(keyReference)
                    .status(SealStatus.SEALED)
                    .sealedAt(LocalDateTime.now())
                    .sealedBy(userId)
                    .scheduledUnsealTime(scheduledUnsealTime)
                    .integrityVerified(true)
                    .build();

            seal = bidSealRepository.save(seal);
            log.info("Bid {} sealed successfully with hash: {}", bidId, contentHash);

            return toDTO(seal);
        } catch (Exception e) {
            log.error("Failed to seal bid: {}", bidId, e);
            throw new RuntimeException("Failed to seal bid", e);
        }
    }

    @Override
    @Transactional
    public BidSealDTO unsealBid(Long bidId, Long userId) {
        log.info("Unsealing bid: {} by user: {}", bidId, userId);

        BidSeal seal = bidSealRepository.findByBidId(bidId)
                .orElseThrow(() -> new EntityNotFoundException("No seal found for bid: " + bidId));

        if (seal.getStatus() != SealStatus.SEALED) {
            throw new IllegalStateException("Bid is not in SEALED status");
        }

        if (LocalDateTime.now().isBefore(seal.getScheduledUnsealTime())) {
            throw new IllegalStateException(
                    "Cannot unseal before scheduled time: " + seal.getScheduledUnsealTime());
        }

        boolean integrityValid = verifyIntegrity(seal);
        seal.setStatus(integrityValid ? SealStatus.UNSEALED : SealStatus.TAMPER_DETECTED);
        seal.setUnsealedAt(LocalDateTime.now());
        seal.setUnsealedBy(userId);
        seal.setIntegrityVerified(integrityValid);

        seal = bidSealRepository.save(seal);

        if (!integrityValid) {
            log.warn("TAMPER DETECTED for bid: {}! Content hash mismatch.", bidId);
        } else {
            log.info("Bid {} unsealed successfully. Integrity verified.", bidId);
        }

        return toDTO(seal);
    }

    @Override
    @Transactional
    public List<BidSealDTO> unsealAllBidsForTender(Long tenderId, Long userId) {
        log.info("Opening all sealed bids for tender: {} by user: {}", tenderId, userId);

        List<BidSeal> sealedBids = bidSealRepository.findByTenderIdAndStatus(tenderId, SealStatus.SEALED);

        if (sealedBids.isEmpty()) {
            log.info("No sealed bids found for tender: {}", tenderId);
            return List.of();
        }

        // Verify all bids can be unsealed (deadline has passed)
        LocalDateTime now = LocalDateTime.now();
        for (BidSeal seal : sealedBids) {
            if (now.isBefore(seal.getScheduledUnsealTime())) {
                throw new IllegalStateException(
                        "Cannot open bids before scheduled time: " + seal.getScheduledUnsealTime());
            }
        }

        return sealedBids.stream()
                .map(seal -> {
                    boolean integrityValid = verifyIntegrity(seal);
                    seal.setStatus(integrityValid ? SealStatus.UNSEALED : SealStatus.TAMPER_DETECTED);
                    seal.setUnsealedAt(now);
                    seal.setUnsealedBy(userId);
                    seal.setIntegrityVerified(integrityValid);
                    bidSealRepository.save(seal);

                    if (!integrityValid) {
                        log.warn("TAMPER DETECTED for bid: {} in tender: {}", seal.getBidId(), tenderId);
                    }
                    return toDTO(seal);
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean verifyBidIntegrity(Long bidId) {
        BidSeal seal = bidSealRepository.findByBidId(bidId)
                .orElseThrow(() -> new EntityNotFoundException("No seal found for bid: " + bidId));
        return verifyIntegrity(seal);
    }

    @Override
    public BidSealDTO getBidSealStatus(Long bidId) {
        BidSeal seal = bidSealRepository.findByBidId(bidId)
                .orElseThrow(() -> new EntityNotFoundException("No seal found for bid: " + bidId));
        return toDTO(seal);
    }

    @Override
    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    @Transactional
    public void processScheduledUnseals() {
        log.info("Processing scheduled bid unseals");

        List<BidSeal> readyForOpening = bidSealRepository
                .findSealedBidsReadyForOpening(SealStatus.SEALED, LocalDateTime.now());

        for (BidSeal seal : readyForOpening) {
            try {
                boolean integrityValid = verifyIntegrity(seal);
                seal.setStatus(integrityValid ? SealStatus.UNSEALED : SealStatus.TAMPER_DETECTED);
                seal.setUnsealedAt(LocalDateTime.now());
                seal.setIntegrityVerified(integrityValid);
                bidSealRepository.save(seal);

                log.info("Auto-unsealed bid: {}, integrity: {}", seal.getBidId(), integrityValid);
            } catch (Exception e) {
                log.error("Failed to auto-unseal bid: {}", seal.getBidId(), e);
            }
        }

        log.info("Processed {} scheduled unseals", readyForOpening.size());
    }

    private String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }

    private String encrypt(String content, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

        // Prepend IV to encrypted content
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    private boolean verifyIntegrity(BidSeal seal) {
        try {
            Bid bid = bidRepository.findById(seal.getBidId())
                    .orElseThrow(() -> new EntityNotFoundException("Bid not found: " + seal.getBidId()));
            String currentContent = objectMapper.writeValueAsString(bid);
            String currentHash = computeHash(currentContent);
            return currentHash.equals(seal.getContentHash());
        } catch (Exception e) {
            log.error("Failed to verify integrity for bid: {}", seal.getBidId(), e);
            return false;
        }
    }

    private BidSealDTO toDTO(BidSeal seal) {
        return BidSealDTO.builder()
                .id(seal.getId())
                .bidId(seal.getBidId())
                .tenderId(seal.getTenderId())
                .contentHash(seal.getContentHash())
                .encryptionAlgorithm(seal.getEncryptionAlgorithm())
                .status(seal.getStatus())
                .sealedAt(seal.getSealedAt())
                .sealedBy(seal.getSealedBy())
                .unsealedAt(seal.getUnsealedAt())
                .unsealedBy(seal.getUnsealedBy())
                .scheduledUnsealTime(seal.getScheduledUnsealTime())
                .integrityVerified(seal.getIntegrityVerified())
                .build();
    }
}
