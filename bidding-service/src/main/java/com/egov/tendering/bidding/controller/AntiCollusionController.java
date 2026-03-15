package com.egov.tendering.bidding.controller;

import com.egov.tendering.bidding.dal.dto.AntiCollusionReport;
import com.egov.tendering.bidding.service.AntiCollusionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/anti-collusion")
@RequiredArgsConstructor
@Slf4j
public class AntiCollusionController {

    private final AntiCollusionService antiCollusionService;

    @GetMapping("/tender/{tenderId}/analyze")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'COMMITTEE')")
    public ResponseEntity<AntiCollusionReport> analyzeForCollusion(@PathVariable Long tenderId) {
        log.info("Request to analyze collusion for tender: {}", tenderId);
        AntiCollusionReport report = antiCollusionService.analyzeForCollusion(tenderId);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/bid/{bidId}/flag")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<Void> flagBid(
            @PathVariable Long bidId,
            @RequestParam String reason) {
        log.info("Request to flag bid: {} reason: {}", bidId, reason);
        antiCollusionService.flagBid(bidId, reason);
        return ResponseEntity.ok().build();
    }
}
