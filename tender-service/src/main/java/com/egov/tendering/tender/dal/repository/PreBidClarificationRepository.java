package com.egov.tendering.tender.dal.repository;

import com.egov.tendering.tender.dal.model.PreBidClarification;
import com.egov.tendering.tender.dal.model.PreBidClarification.ClarificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreBidClarificationRepository extends JpaRepository<PreBidClarification, Long> {

    List<PreBidClarification> findByTenderIdOrderByAskedAtDesc(Long tenderId);

    List<PreBidClarification> findByTenderIdAndIsPublicTrueOrderByAskedAtDesc(Long tenderId);

    List<PreBidClarification> findByTenderIdAndStatusOrderByAskedAtDesc(Long tenderId, ClarificationStatus status);

    List<PreBidClarification> findByTenderIdAndAskedByOrderByAskedAtDesc(Long tenderId, Long askedBy);

    long countByTenderIdAndStatus(Long tenderId, ClarificationStatus status);
}
