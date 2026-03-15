package com.egov.tendering.bidding.dal.repository;

import com.egov.tendering.bidding.dal.model.BidSubmissionMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BidSubmissionMetadataRepository extends JpaRepository<BidSubmissionMetadata, Long> {

    Optional<BidSubmissionMetadata> findByBidId(Long bidId);

    List<BidSubmissionMetadata> findByTenderId(Long tenderId);

    List<BidSubmissionMetadata> findByTendererId(Long tendererId);

    List<BidSubmissionMetadata> findByFlaggedTrue();

    @Query("SELECT bsm FROM BidSubmissionMetadata bsm WHERE bsm.tenderId = :tenderId AND bsm.ipAddress = :ipAddress AND bsm.tendererId <> :tendererId")
    List<BidSubmissionMetadata> findSameIpDifferentTenderer(@Param("tenderId") Long tenderId,
                                                            @Param("ipAddress") String ipAddress,
                                                            @Param("tendererId") Long tendererId);

    @Query("SELECT bsm FROM BidSubmissionMetadata bsm WHERE bsm.tenderId = :tenderId AND bsm.deviceFingerprint = :fingerprint AND bsm.tendererId <> :tendererId")
    List<BidSubmissionMetadata> findSameDeviceDifferentTenderer(@Param("tenderId") Long tenderId,
                                                                @Param("fingerprint") String fingerprint,
                                                                @Param("tendererId") Long tendererId);
}
