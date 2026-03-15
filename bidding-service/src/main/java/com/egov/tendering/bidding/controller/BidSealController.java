package com.egov.tendering.bidding.controller;

import com.egov.tendering.bidding.dal.dto.BidSealDTO;
import com.egov.tendering.bidding.service.BidSealingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
@Slf4j
public class BidSealController {

    private final BidSealingService bidSealingService;

    @PostMapping("/{bidId}/seal")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE')")
    public ResponseEntity<BidSealDTO> sealBid(
            @PathVariable Long bidId,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        log.info("Request to seal bid: {} by user: {}", bidId, userId);
        BidSealDTO seal = bidSealingService.sealBid(bidId, userId);
        return new ResponseEntity<>(seal, HttpStatus.CREATED);
    }

    @PostMapping("/{bidId}/unseal")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<BidSealDTO> unsealBid(
            @PathVariable Long bidId,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        log.info("Request to unseal bid: {} by user: {}", bidId, userId);
        BidSealDTO seal = bidSealingService.unsealBid(bidId, userId);
        return ResponseEntity.ok(seal);
    }

    @PostMapping("/tender/{tenderId}/unseal-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<List<BidSealDTO>> unsealAllBidsForTender(
            @PathVariable Long tenderId,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        log.info("Request to unseal all bids for tender: {} by user: {}", tenderId, userId);
        List<BidSealDTO> seals = bidSealingService.unsealAllBidsForTender(tenderId, userId);
        return ResponseEntity.ok(seals);
    }

    @GetMapping("/{bidId}/seal/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'COMMITTEE')")
    public ResponseEntity<Boolean> verifyBidIntegrity(@PathVariable Long bidId) {
        log.info("Request to verify integrity of bid: {}", bidId);
        boolean isValid = bidSealingService.verifyBidIntegrity(bidId);
        return ResponseEntity.ok(isValid);
    }

    @GetMapping("/{bidId}/seal/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'COMMITTEE')")
    public ResponseEntity<BidSealDTO> getBidSealStatus(@PathVariable Long bidId) {
        log.info("Request to get seal status for bid: {}", bidId);
        BidSealDTO seal = bidSealingService.getBidSealStatus(bidId);
        return ResponseEntity.ok(seal);
    }

    private Long getUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        if (userIdClaim != null) {
            return Long.parseLong(userIdClaim.toString());
        }
        return Long.parseLong(jwt.getSubject());
    }
}
