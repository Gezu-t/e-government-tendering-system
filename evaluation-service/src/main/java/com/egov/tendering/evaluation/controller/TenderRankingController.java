package com.egov.tendering.evaluation.controller;

import com.egov.tendering.evaluation.dal.dto.TenderRankingDTO;
import com.egov.tendering.evaluation.service.EvaluationService;
import com.egov.tendering.evaluation.service.TenderRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for tender ranking operations.
 */
@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
@Slf4j
public class TenderRankingController {

    private final TenderRankingService rankingService;
    private final EvaluationService evaluationService;

    /**
     * Calculates bid rankings for a tender based on evaluations.
     *
     * @param tenderId The tender ID
     * @return List of bid rankings
     */
    @PostMapping("/tenders/{tenderId}/calculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<List<TenderRankingDTO>> calculateBidRankings(@PathVariable Long tenderId) {
        log.info("Calculating bid rankings for tender ID: {}", tenderId);

        var evaluations = evaluationService.getEvaluationsByTenderId(tenderId);
        var rankings = rankingService.calculateBidRankings(tenderId, evaluations);

        return ResponseEntity.ok(rankings);
    }

    /**
     * Gets all bid rankings for a tender.
     *
     * @param tenderId The tender ID
     * @return List of bid rankings
     */
    @GetMapping("/tenders/{tenderId}")
    public ResponseEntity<List<TenderRankingDTO>> getBidRankingsByTender(@PathVariable Long tenderId) {
        log.info("Getting bid rankings for tender ID: {}", tenderId);

        var rankings = rankingService.getBidRankingsByTender(tenderId);

        return ResponseEntity.ok(rankings);
    }

    /**
     * Gets all winning bids for a tender.
     *
     * @param tenderId The tender ID
     * @return List of winning bids
     */
    @GetMapping("/tenders/{tenderId}/winners")
    public ResponseEntity<List<TenderRankingDTO>> getWinningBids(@PathVariable Long tenderId) {
        log.info("Getting winning bids for tender ID: {}", tenderId);

        var winners = rankingService.getWinningBids(tenderId);

        return ResponseEntity.ok(winners);
    }

    /**
     * Clears all rankings for a tender.
     *
     * @param tenderId The tender ID
     * @return Empty response
     */
    @DeleteMapping("/tenders/{tenderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<Void> clearRankings(@PathVariable Long tenderId) {
        log.info("Clearing rankings for tender ID: {}", tenderId);

        rankingService.clearRankings(tenderId);

        return ResponseEntity.noContent().build();
    }
}
