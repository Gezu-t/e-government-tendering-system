package com.egov.tendering.bidding.service;

import com.egov.tendering.bidding.dal.dto.DigitalSignatureDTO;

import java.util.List;

public interface DigitalSignatureService {

    /**
     * Signs an entity (bid or contract) with a digital signature.
     * Creates a SHA-256 hash of the entity content and signs it.
     */
    DigitalSignatureDTO signEntity(String entityType, Long entityId, Long signerId, String signerName);

    /**
     * Verifies the digital signature of an entity.
     */
    DigitalSignatureDTO verifySignature(Long signatureId, Long verifierId);

    /**
     * Rejects a digital signature with a reason.
     */
    DigitalSignatureDTO rejectSignature(Long signatureId, Long verifierId, String reason);

    /**
     * Gets all signatures for an entity.
     */
    List<DigitalSignatureDTO> getSignaturesForEntity(String entityType, Long entityId);

    /**
     * Checks if an entity has been signed by a specific user.
     */
    boolean isSignedBy(String entityType, Long entityId, Long signerId);

    /**
     * Checks if all required signatures are present and verified.
     */
    boolean areAllSignaturesVerified(String entityType, Long entityId);
}
