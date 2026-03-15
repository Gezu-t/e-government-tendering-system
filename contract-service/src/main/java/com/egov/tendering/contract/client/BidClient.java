package com.egov.tendering.contract.client;

import com.egov.tendering.dto.BidDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "${app.feign.bid-service}")
public interface BidClient {

    @GetMapping("/api/bids/tender/{tenderId}/status/AWARDED")
    List<BidDTO> getAwardedBidsByTender(@PathVariable Long tenderId);
}
