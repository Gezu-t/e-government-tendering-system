package com.egov.tendering.bidding.dal.repository;

import com.egov.tendering.bidding.dal.model.BidEnvelope;
import com.egov.tendering.bidding.dal.model.BidEnvelope.EnvelopeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BidEnvelopeRepository extends JpaRepository<BidEnvelope, Long> {

    List<BidEnvelope> findByBidId(Long bidId);

    Optional<BidEnvelope> findByBidIdAndEnvelopeType(Long bidId, EnvelopeType envelopeType);

    List<BidEnvelope> findByBidIdAndIsSealed(Long bidId, Boolean isSealed);
}
