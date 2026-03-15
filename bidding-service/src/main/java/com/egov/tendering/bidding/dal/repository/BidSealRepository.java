package com.egov.tendering.bidding.dal.repository;

import com.egov.tendering.bidding.dal.model.BidSeal;
import com.egov.tendering.bidding.dal.model.SealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BidSealRepository extends JpaRepository<BidSeal, Long> {

    Optional<BidSeal> findByBidId(Long bidId);

    List<BidSeal> findByTenderIdAndStatus(Long tenderId, SealStatus status);

    @Query("SELECT bs FROM BidSeal bs WHERE bs.status = :status AND bs.scheduledUnsealTime <= :now")
    List<BidSeal> findSealedBidsReadyForOpening(@Param("status") SealStatus status,
                                                 @Param("now") LocalDateTime now);

    boolean existsByBidId(Long bidId);
}
