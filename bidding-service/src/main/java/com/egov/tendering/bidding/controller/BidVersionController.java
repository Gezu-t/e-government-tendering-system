package com.egov.tendering.bidding.controller;

import com.egov.tendering.bidding.dal.dto.BidDTO;
import com.egov.tendering.bidding.dal.dto.BidVersionDTO;
import com.egov.tendering.bidding.service.BidVersionService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/bids/{bidId}/versions")
@RequiredArgsConstructor
@Slf4j
public class BidVersionController {

    private final BidVersionService bidVersionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR') or @bidAccessSecurityUtil.isBidOwner(#bidId)")
    public ResponseEntity<List<BidVersionDTO>> getBidVersions(@PathVariable Long bidId) {
        log.info("Getting version history for bid ID: {}", bidId);
        List<BidVersionDTO> versions = bidVersionService.getBidVersions(bidId);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{versionNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR') or @bidAccessSecurityUtil.isBidOwner(#bidId)")
    public ResponseEntity<BidVersionDTO> getBidVersion(
            @PathVariable Long bidId,
            @PathVariable Integer versionNumber) {
        log.info("Getting version {} for bid ID: {}", versionNumber, bidId);
        BidVersionDTO version = bidVersionService.getBidVersion(bidId, versionNumber);
        return ResponseEntity.ok(version);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR') or @bidAccessSecurityUtil.isBidOwner(#bidId)")
    public ResponseEntity<BidVersionDTO> createBidVersion(
            @PathVariable Long bidId,
            @Valid @RequestBody BidDTO bidData,
            @RequestParam String changeSummary,
            @AuthenticationPrincipal Jwt jwt) {
        Long createdBy = getUserId(jwt);
        log.info("Creating version snapshot for bid ID: {}", bidId);
        BidVersionDTO version = bidVersionService.saveBidVersion(bidId, bidData, changeSummary, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
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
