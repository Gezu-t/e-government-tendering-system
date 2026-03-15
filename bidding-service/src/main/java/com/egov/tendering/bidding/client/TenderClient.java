package com.egov.tendering.bidding.client;

import com.egov.tendering.dto.TenderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${app.feign.tender-service}")
public interface TenderClient {

    @GetMapping("/api/tenders/{tenderId}")
    TenderDTO getTenderById(@PathVariable Long tenderId);
}
