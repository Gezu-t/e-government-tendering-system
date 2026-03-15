package com.egov.tendering.evaluation.controller;

import com.egov.tendering.evaluation.dal.dto.AllocationResultDTO;
import com.egov.tendering.evaluation.dal.dto.AllocationRequest;
import com.egov.tendering.evaluation.service.AllocationService;
import com.egov.tendering.evaluation.service.EvaluationService;
import com.egov.tendering.evaluation.service.TenderRankingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for allocation operations.
 */
@RestController
@RequestMapping("/api/allocations")
@RequiredArgsConstructor
@Slf4j
public class AllocationController {

    private final AllocationService allocationService;
    private final TenderRankingService rankingService;
    private final EvaluationService evaluationService;

    /**
     * Allocates items to a single winner for a tender.
     *
     * @param tenderId The tender ID
     * @return List of allocated items
     */
    @PostMapping("/tenders/{tenderId}/single")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<List<AllocationResultDTO>> allocateSingleWinner(@PathVariable Long tenderId) {
        log.info("Allocating single winner for tender ID: {}", tenderId);

        // Get rankings for the tender
        var rankings = rankingService.getBidRankingsByTender(tenderId);
        var allocations = allocationService.allocateSingleWinner(tenderId, rankings);

        return ResponseEntity.ok(allocations);
    }

    /**
     * Allocates items cooperatively for a tender.
     *
     * @param tenderId The tender ID
     * @return List of allocated items
     */
    @PostMapping("/tenders/{tenderId}/cooperative")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<List<AllocationResultDTO>> allocateCooperatively(@PathVariable Long tenderId) {
        log.info("Allocating cooperatively for tender ID: {}", tenderId);

        // Get evaluations for the tender
        var evaluations = evaluationService.getEvaluationsByTenderId(tenderId);
        var allocations = allocationService.allocateCooperative(tenderId, evaluations);

        return ResponseEntity.ok(allocations);
    }

    /**
     * Allocates items competitively for a tender.
     *
     * @param tenderId The tender ID
     * @param request The allocation request details
     * @return List of allocated items
     */
    @PostMapping("/tenders/{tenderId}/competitive")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<List<AllocationResultDTO>> allocateCompetitively(
            @PathVariable Long tenderId,
            @Valid @RequestBody AllocationRequest request) {

        log.info("Allocating competitively for tender ID: {} with cutoff score: {} and average allocation: {}",
                tenderId, request.getCutoffScore(), request.isAverageAllocation());

        // Get rankings for the tender
        var rankings = rankingService.getBidRankingsByTender(tenderId);
        var allocations = allocationService.allocateCompetitive(
                tenderId, rankings, request.getCutoffScore(), request.isAverageAllocation());

        return ResponseEntity.ok(allocations);
    }

    /**
     * Gets all allocations for a tender.
     *
     * @param tenderId The tender ID
     * @return List of allocations
     */
    @GetMapping("/tenders/{tenderId}")
    public ResponseEntity<List<AllocationResultDTO>> getAllocationsByTender(@PathVariable Long tenderId) {
        log.info("Getting allocations for tender ID: {}", tenderId);

        var allocations = allocationService.getAllocationsByTender(tenderId);

        return ResponseEntity.ok(allocations);
    }

    /**
     * Gets all allocations for a bid.
     *
     * @param bidId The bid ID
     * @return List of allocations
     */
    @GetMapping("/bids/{bidId}")
    public ResponseEntity<List<AllocationResultDTO>> getAllocationsByBid(@PathVariable Long bidId) {
        log.info("Getting allocations for bid ID: {}", bidId);

        var allocations = allocationService.getAllocationsByBid(bidId);

        return ResponseEntity.ok(allocations);
    }

    /**
     * Clears all allocations for a tender.
     *
     * @param tenderId The tender ID
     * @return Empty response
     */
    @DeleteMapping("/tenders/{tenderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<Void> clearAllocations(@PathVariable Long tenderId) {
        log.info("Clearing allocations for tender ID: {}", tenderId);

        allocationService.clearAllocations(tenderId);

        return ResponseEntity.noContent().build();
    }
}
