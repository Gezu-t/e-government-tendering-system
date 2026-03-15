package com.egov.tendering.bidding.service;

import com.egov.tendering.bidding.dal.dto.BidSealDTO;

import java.util.List;

public interface BidSealingService {

    /**
     * Seals a bid by encrypting its content and generating an integrity hash.
     * Bids are sealed upon submission and remain sealed until the tender's submission deadline.
     */
    BidSealDTO sealBid(Long bidId, Long userId);

    /**
     * Unseals a single bid after the tender deadline has passed.
     * Only ADMIN or EVALUATOR roles can unseal bids.
     */
    BidSealDTO unsealBid(Long bidId, Long userId);

    /**
     * Unseals all bids for a tender (bid opening ceremony).
     * Only allowed after the tender's submission deadline has passed.
     */
    List<BidSealDTO> unsealAllBidsForTender(Long tenderId, Long userId);

    /**
     * Verifies the integrity of a sealed bid by comparing its hash.
     */
    boolean verifyBidIntegrity(Long bidId);

    /**
     * Gets the seal status for a bid.
     */
    BidSealDTO getBidSealStatus(Long bidId);

    /**
     * Automatically unseals bids whose scheduled unseal time has passed.
     */
    void processScheduledUnseals();
}
