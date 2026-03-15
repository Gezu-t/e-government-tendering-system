package com.egov.tendering.bidding.controller;


import com.egov.tendering.bidding.dal.dto.BidItemDTO;
import com.egov.tendering.bidding.dal.dto.BidItemRequest;
import com.egov.tendering.bidding.service.BidItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bid Items", description = "APIs for managing bid items")
public class BidItemController {

    private final BidItemService bidItemService;

    @PostMapping("/{bidId}/items")
    @Operation(summary = "Save bid items for a bid",
            description = "Save a list of items for a specific bid")
    @ApiResponse(responseCode = "201", description = "Bid items saved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @PreAuthorize("hasRole('TENDERER') and @bidAccessSecurityUtil.isBidOwner(#bidId)")
    public ResponseEntity<Void> saveBidItems(
            @PathVariable @Parameter(description = "ID of the bid") Long bidId,
            @RequestBody @Valid List<BidItemDTO> items) {
        log.info("REST request to save {} bid items for bid ID: {}", items.size(), bidId);
        bidItemService.saveBidItems(bidId, items);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{bidId}/items")
    @Operation(summary = "Get all items for a bid",
            description = "Retrieves all items associated with the specified bid")
    @ApiResponse(responseCode = "200", description = "List of bid items retrieved successfully")
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN') or @bidAccessSecurityUtil.isBidOwner(#bidId)")
    public ResponseEntity<List<BidItemRequest>> getBidItems(
            @PathVariable @Parameter(description = "ID of the bid") Long bidId) {
        log.info("REST request to get items for bid ID: {}", bidId);
        List<BidItemRequest> bidItems = bidItemService.getBidItems(bidId);
        return ResponseEntity.ok(bidItems);
    }
}
