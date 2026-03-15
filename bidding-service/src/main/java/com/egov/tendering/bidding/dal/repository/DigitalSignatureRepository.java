package com.egov.tendering.bidding.dal.repository;

import com.egov.tendering.bidding.dal.model.DigitalSignature;
import com.egov.tendering.bidding.dal.model.SignatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DigitalSignatureRepository extends JpaRepository<DigitalSignature, Long> {

    List<DigitalSignature> findByEntityTypeAndEntityId(String entityType, Long entityId);

    Optional<DigitalSignature> findByEntityTypeAndEntityIdAndSignerId(String entityType, Long entityId, Long signerId);

    List<DigitalSignature> findBySignerId(Long signerId);

    List<DigitalSignature> findByEntityTypeAndEntityIdAndStatus(String entityType, Long entityId, SignatureStatus status);

    boolean existsByEntityTypeAndEntityIdAndSignerId(String entityType, Long entityId, Long signerId);
}
