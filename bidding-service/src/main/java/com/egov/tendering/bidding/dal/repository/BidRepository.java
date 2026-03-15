package com.egov.tendering.bidding.dal.repository;

import com.egov.tendering.bidding.dal.model.Bid;
import com.egov.tendering.bidding.dal.model.BidStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByTenderIdAndStatus(Long tenderId, BidStatus status);

    List<Bid> findByTendererIdAndStatus(Long tendererId, BidStatus status);

    Page<Bid> findByTendererId(Long tendererId, Pageable pageable);

    Page<Bid> findByTenderId(Long tenderId, Pageable pageable);

    boolean existsByTenderIdAndTendererId(Long tenderId, Long tendererId);

    Optional<Bid> findByTenderIdAndTendererId(Long tenderId, Long tendererId);

    /**
     * Check if a bid exists for a specific tender ID.
     *
     * @param tenderId the ID of the tender
     * @return true if a bid exists for the given tender ID, otherwise false
     */
    boolean existsByTenderId(Long tenderId);


    /**
     * Find all bids for a specific tender with any status except the specified one
     */
    List<Bid> findByTenderIdAndStatusNot(Long tenderId, BidStatus status);

}
