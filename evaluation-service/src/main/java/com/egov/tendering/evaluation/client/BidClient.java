package com.egov.tendering.evaluation.client;

import com.egov.tendering.dto.BidDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for accessing Bidding Service APIs
 */
@FeignClient(name = "${app.feign.bid-service}")
public interface BidClient {

    /**
     * Gets a bid by ID
     *
     * @param bidId The bid ID
     * @return The bid details
     */
    @GetMapping("/api/bids/{bidId}")
    BidDTO getBidById(@PathVariable Long bidId);
}
