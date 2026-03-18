package com.egov.tendering.bidding.service.impl;

import com.egov.tendering.bidding.config.BidSealingProperties;
import com.egov.tendering.bidding.dal.dto.BidSealDTO;
import com.egov.tendering.bidding.dal.model.*;
import com.egov.tendering.bidding.dal.repository.BidRepository;
import com.egov.tendering.bidding.dal.repository.BidSealRepository;
import com.egov.tendering.bidding.service.BidSealingService;
import com.egov.tendering.bidding.service.TenderWorkflowGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bid sealing service using envelope encryption.
 *
 * <p><b>Key management model:</b>
 * <ol>
 *   <li>A random 256-bit content encryption key (CEK) is generated per bid.</li>
 *   <li>The CEK is wrapped (encrypted) with a master key loaded from
 *       {@code bidding.seal.master-key-base64} (env: {@code SEALING_MASTER_KEY}).
 *       Only the wrapped CEK is persisted in {@code seal_key_reference}.</li>
 *   <li>The bid payload is encrypted with the CEK and stored in {@code encrypted_content}.</li>
 *   <li>On unseal the master key unwraps the CEK, the CEK decrypts the payload,
 *       and the resulting hash is compared against {@code content_hash}.</li>
 * </ol>
 *
 * <p>Integrity is verified entirely against the immutable sealed artifact, never against
 * the live {@code Bid} entity, eliminating false-positive tamper detections from ORM updates.
 */
@Service
@Slf4j
public class BidSealingServiceImpl implements BidSealingService {

    private final BidSealRepository bidSealRepository;
    private final BidRepository bidRepository;
    private final TenderWorkflowGuard tenderWorkflowGuard;
    private final ObjectMapper objectMapper;
    private final BidSealingProperties sealProps;
    private final SecretKey masterKey;

    public BidSealingServiceImpl(BidSealRepository bidSealRepository,
                                 BidRepository bidRepository,
                                 TenderWorkflowGuard tenderWorkflowGuard,
                                 ObjectMapper objectMapper,
                                 BidSealingProperties sealProps) {
        this.bidSealRepository = bidSealRepository;
        this.bidRepository = bidRepository;
        this.tenderWorkflowGuard = tenderWorkflowGuard;
        this.objectMapper = objectMapper;
        this.sealProps = sealProps;
        this.masterKey = deriveMasterKey(sealProps.getMasterKeyBase64());
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

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

            // Generate a random content encryption key (CEK)
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey cek = keyGen.generateKey();

            // Encrypt the bid payload with the CEK
            String encryptedContent = encrypt(bidContent, cek);

            // Wrap the CEK with the master key — only the wrapped form is stored
            String wrappedCek = wrapKey(cek);

            LocalDateTime scheduledUnsealTime = tenderWorkflowGuard.getSubmissionDeadline(bid.getTenderId());

            BidSeal seal = BidSeal.builder()
                    .bidId(bidId)
                    .tenderId(bid.getTenderId())
                    .contentHash(contentHash)
                    .encryptedContent(encryptedContent)
                    .encryptionAlgorithm(sealProps.getEncryptionAlgorithm())
                    .sealKeyReference(wrappedCek)
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
            log.warn("TAMPER DETECTED for bid: {}! Integrity check failed.", bidId);
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
    @Scheduled(cron = "${bidding.seal.unseal-check-cron:0 */15 * * * *}")
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

    // -----------------------------------------------------------------------
    // Crypto helpers
    // -----------------------------------------------------------------------

    /**
     * Encrypts plaintext with the given key using AES-GCM.
     * The IV is prepended to the ciphertext and the whole thing is base64-encoded.
     */
    private String encrypt(String plaintext, SecretKey key) throws Exception {
        byte[] iv = new byte[sealProps.getGcmIvLength()];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(sealProps.getEncryptionAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(sealProps.getGcmTagLength(), iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypts a base64-encoded (IV || ciphertext) blob produced by {@link #encrypt}.
     */
    private String decrypt(String encryptedBase64, SecretKey key) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);
        byte[] iv = Arrays.copyOfRange(combined, 0, sealProps.getGcmIvLength());
        byte[] ciphertext = Arrays.copyOfRange(combined, sealProps.getGcmIvLength(), combined.length);

        Cipher cipher = Cipher.getInstance(sealProps.getEncryptionAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(sealProps.getGcmTagLength(), iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    /**
     * Wraps (encrypts) a content encryption key with the master key using AES-GCM.
     * Returns a base64-encoded (IV || wrapped-key) blob.
     */
    private String wrapKey(SecretKey cek) throws Exception {
        return encrypt(Base64.getEncoder().encodeToString(cek.getEncoded()),
                masterKey);
    }

    /**
     * Unwraps a CEK that was wrapped by {@link #wrapKey}.
     */
    private SecretKey unwrapKey(String wrappedKeyBase64) throws Exception {
        String cekBase64 = decrypt(wrappedKeyBase64, masterKey);
        byte[] keyBytes = Base64.getDecoder().decode(cekBase64);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Verifies integrity by decrypting the sealed payload and comparing its hash
     * against the stored {@code contentHash}.
     *
     * <p>This operates entirely on the immutable sealed artifact — it never reads
     * the live {@code Bid} entity, eliminating false-positive tamper detections
     * caused by ORM-managed fields (e.g. {@code updatedAt}, {@code version}).
     */
    private boolean verifyIntegrity(BidSeal seal) {
        try {
            SecretKey cek = unwrapKey(seal.getSealKeyReference());
            String decryptedContent = decrypt(seal.getEncryptedContent(), cek);
            String decryptedHash = computeHash(decryptedContent);
            return decryptedHash.equals(seal.getContentHash());
        } catch (Exception e) {
            log.error("Integrity verification failed for bid: {} — possible tampering or key mismatch",
                    seal.getBidId(), e);
            return false;
        }
    }

    private String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance(sealProps.getHashAlgorithm());
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }

    private static SecretKey deriveMasterKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "Bid sealing master key (bidding.seal.master-key-base64 / SEALING_MASTER_KEY) must be configured");
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "Bid sealing master key must be exactly 256 bits (32 bytes base64-encoded)");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    // -----------------------------------------------------------------------
    // DTO mapping
    // -----------------------------------------------------------------------

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
