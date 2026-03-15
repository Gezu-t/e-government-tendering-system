package com.egov.tendering.bidding.service.impl;

import com.egov.tendering.bidding.dal.dto.DigitalSignatureDTO;
import com.egov.tendering.bidding.dal.model.DigitalSignature;
import com.egov.tendering.bidding.dal.model.SignatureStatus;
import com.egov.tendering.bidding.dal.repository.BidRepository;
import com.egov.tendering.bidding.dal.repository.DigitalSignatureRepository;
import com.egov.tendering.bidding.service.DigitalSignatureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalSignatureServiceImpl implements DigitalSignatureService {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String HASH_ALGORITHM = "SHA-256";

    private final DigitalSignatureRepository signatureRepository;
    private final BidRepository bidRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public DigitalSignatureDTO signEntity(String entityType, Long entityId, Long signerId, String signerName) {
        log.info("Signing {} with ID: {} by user: {}", entityType, entityId, signerId);

        if (signatureRepository.existsByEntityTypeAndEntityIdAndSignerId(entityType, entityId, signerId)) {
            throw new IllegalStateException("Entity already signed by this user");
        }

        try {
            String contentHash = computeEntityHash(entityType, entityId);

            // Generate key pair for signing
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            KeyPair keyPair = keyPairGen.generateKeyPair();

            // Sign the hash
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(keyPair.getPrivate());
            signature.update(contentHash.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();
            String signatureValue = Base64.getEncoder().encodeToString(signatureBytes);

            // Store public key serial for verification
            String certificateSerial = Base64.getEncoder()
                    .encodeToString(keyPair.getPublic().getEncoded());

            DigitalSignature digitalSignature = DigitalSignature.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .signerId(signerId)
                    .signerName(signerName)
                    .signatureValue(signatureValue)
                    .contentHash(contentHash)
                    .algorithm(SIGNATURE_ALGORITHM)
                    .certificateSerial(certificateSerial)
                    .status(SignatureStatus.PENDING_VERIFICATION)
                    .signedAt(LocalDateTime.now())
                    .build();

            digitalSignature = signatureRepository.save(digitalSignature);
            log.info("Entity {} {} signed successfully by {}", entityType, entityId, signerName);

            return toDTO(digitalSignature);
        } catch (Exception e) {
            log.error("Failed to sign entity {} {}", entityType, entityId, e);
            throw new RuntimeException("Failed to create digital signature", e);
        }
    }

    @Override
    @Transactional
    public DigitalSignatureDTO verifySignature(Long signatureId, Long verifierId) {
        log.info("Verifying signature: {} by user: {}", signatureId, verifierId);

        DigitalSignature sig = signatureRepository.findById(signatureId)
                .orElseThrow(() -> new EntityNotFoundException("Signature not found: " + signatureId));

        try {
            // Recompute content hash and compare
            String currentHash = computeEntityHash(sig.getEntityType(), sig.getEntityId());
            boolean hashMatch = currentHash.equals(sig.getContentHash());

            if (hashMatch) {
                sig.setStatus(SignatureStatus.VERIFIED);
                log.info("Signature {} verified successfully", signatureId);
            } else {
                sig.setStatus(SignatureStatus.REJECTED);
                sig.setRejectionReason("Content hash mismatch - document may have been tampered with");
                log.warn("Signature {} verification FAILED - hash mismatch", signatureId);
            }

            sig.setVerifiedAt(LocalDateTime.now());
            sig.setVerifiedBy(verifierId);
            sig = signatureRepository.save(sig);

            return toDTO(sig);
        } catch (Exception e) {
            log.error("Failed to verify signature: {}", signatureId, e);
            throw new RuntimeException("Failed to verify signature", e);
        }
    }

    @Override
    @Transactional
    public DigitalSignatureDTO rejectSignature(Long signatureId, Long verifierId, String reason) {
        log.info("Rejecting signature: {} by user: {}", signatureId, verifierId);

        DigitalSignature sig = signatureRepository.findById(signatureId)
                .orElseThrow(() -> new EntityNotFoundException("Signature not found: " + signatureId));

        sig.setStatus(SignatureStatus.REJECTED);
        sig.setVerifiedAt(LocalDateTime.now());
        sig.setVerifiedBy(verifierId);
        sig.setRejectionReason(reason);
        sig = signatureRepository.save(sig);

        return toDTO(sig);
    }

    @Override
    public List<DigitalSignatureDTO> getSignaturesForEntity(String entityType, Long entityId) {
        return signatureRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSignedBy(String entityType, Long entityId, Long signerId) {
        return signatureRepository.existsByEntityTypeAndEntityIdAndSignerId(entityType, entityId, signerId);
    }

    @Override
    public boolean areAllSignaturesVerified(String entityType, Long entityId) {
        List<DigitalSignature> signatures = signatureRepository
                .findByEntityTypeAndEntityId(entityType, entityId);

        if (signatures.isEmpty()) {
            return false;
        }

        return signatures.stream()
                .allMatch(sig -> sig.getStatus() == SignatureStatus.VERIFIED);
    }

    private String computeEntityHash(String entityType, Long entityId) {
        try {
            String content;
            if ("BID".equalsIgnoreCase(entityType)) {
                content = objectMapper.writeValueAsString(
                        bidRepository.findById(entityId)
                                .orElseThrow(() -> new EntityNotFoundException("Bid not found: " + entityId)));
            } else {
                content = entityType + ":" + entityId + ":" + LocalDateTime.now();
            }

            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute entity hash", e);
        }
    }

    private DigitalSignatureDTO toDTO(DigitalSignature sig) {
        return DigitalSignatureDTO.builder()
                .id(sig.getId())
                .entityType(sig.getEntityType())
                .entityId(sig.getEntityId())
                .signerId(sig.getSignerId())
                .signerName(sig.getSignerName())
                .contentHash(sig.getContentHash())
                .algorithm(sig.getAlgorithm())
                .certificateSerial(sig.getCertificateSerial())
                .status(sig.getStatus())
                .signedAt(sig.getSignedAt())
                .verifiedAt(sig.getVerifiedAt())
                .verifiedBy(sig.getVerifiedBy())
                .rejectionReason(sig.getRejectionReason())
                .build();
    }
}
