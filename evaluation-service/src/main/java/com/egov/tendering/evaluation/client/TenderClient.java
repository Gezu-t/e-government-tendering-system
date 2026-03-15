package com.egov.tendering.evaluation.client;

import com.egov.tendering.dto.TenderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for accessing Tender Service APIs
 */
@FeignClient(name = "${app.feign.tender-service}")
public interface TenderClient {

    /**
     * Gets a tender by ID
     *
     * @param tenderId The tender ID
     * @return The tender details
     */
    @GetMapping("/api/tenders/{tenderId}")
    TenderDTO getTenderById(@PathVariable Long tenderId);
}
