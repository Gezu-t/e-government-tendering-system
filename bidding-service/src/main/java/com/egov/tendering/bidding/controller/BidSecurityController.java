package com.egov.tendering.bidding.controller;

import com.egov.tendering.bidding.dal.dto.BidSecurityDTO;
import com.egov.tendering.bidding.dal.dto.BidSecurityRequest;
import com.egov.tendering.bidding.dal.model.SecurityStatus;
import com.egov.tendering.bidding.service.BidSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bid Securities", description = "APIs for managing bid securities")
public class BidSecurityController {

    private final BidSecurityService bidSecurityService;

    @GetMapping("/{bidId}/security")
    @Operation(summary = "Get bid security for a bid",
            description = "Retrieves the security document and details for the specified bid")
    @ApiResponse(responseCode = "200", description = "Bid security retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Bid security not found")
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN') or @bidAccessSecurityUtil.isBidOwner(#bidId)")
    public ResponseEntity<BidSecurityDTO> getBidSecurity(
            @PathVariable @Parameter(description = "ID of the bid") Long bidId) {
        log.info("REST request to get security for bid ID: {}", bidId);
        BidSecurityDTO security = bidSecurityService.getBidSecurityByBidId(bidId);
        return ResponseEntity.ok(security);
    }

    @PostMapping(value = "/{bidId}/security", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add security for a bid",
            description = "Upload security document and add security details for a bid")
    @ApiResponse(responseCode = "201", description = "Bid security added successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters or bid state")
    @ApiResponse(responseCode = "404", description = "Bid not found")
    @PreAuthorize("hasRole('TENDERER') and @bidAccessSecurityUtil.isBidOwner(#bidId)")
    public ResponseEntity<BidSecurityDTO> addBidSecurity(
            @PathVariable @Parameter(description = "ID of the bid") Long bidId,
            @RequestPart("securityData") @Valid BidSecurityRequest request,
            @RequestPart(value = "document", required = false) MultipartFile document) {
        log.info("REST request to add security for bid ID: {}", bidId);
        BidSecurityDTO result = bidSecurityService.addBidSecurity(bidId, request, document);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/security/{securityId}/verify")
    @Operation(summary = "Verify a bid security",
            description = "Verify a bid security and update its status based on the verification result")
    @ApiResponse(responseCode = "200", description = "Bid security verified successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters or security state")
    @ApiResponse(responseCode = "404", description = "Bid security not found")
    @PreAuthorize("hasRole('EVALUATOR')")
    public ResponseEntity<BidSecurityDTO> verifyBidSecurity(
            @PathVariable @Parameter(description = "ID of the security") Long securityId,
            @RequestBody @Valid SecurityVerificationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long evaluatorId = getUserId(jwt);
        log.info("REST request to verify security ID: {} with status: {}", securityId, request.getStatus());

        BidSecurityDTO result = bidSecurityService.verifyBidSecurity(
                securityId,
                request.getStatus(),
                request.getComment(),
                evaluatorId);

        return ResponseEntity.ok(result);
    }

    @PutMapping("/security/{securityId}/status")
    @Operation(summary = "Update bid security status",
            description = "Update the status of a bid security (e.g., to RETURNED or FORFEITED)")
    @ApiResponse(responseCode = "200", description = "Bid security status updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters or security state")
    @ApiResponse(responseCode = "404", description = "Bid security not found")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BidSecurityDTO> updateBidSecurityStatus(
            @PathVariable @Parameter(description = "ID of the security") Long securityId,
            @RequestBody @Valid SecurityStatusRequest request) {
        log.info("REST request to update security ID: {} to status: {}", securityId, request.getStatus());

        BidSecurityDTO result = bidSecurityService.updateBidSecurityStatus(
                securityId,
                request.getStatus());

        return ResponseEntity.ok(result);
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

    // Request DTOs
    @Schema(description = "Request object for verifying a bid security")
    public static class SecurityVerificationRequest {
        @Schema(description = "The verification status", example = "VERIFIED", required = true)
        @NotNull(message = "Status is required")
        private SecurityStatus status;

        @Schema(description = "Verification comment or reason", example = "Document is valid and matches the issuer records")
        private String comment;

        public SecurityStatus getStatus() {
            return status;
        }

        public void setStatus(SecurityStatus status) {
            this.status = status;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    @Schema(description = "Request object for updating bid security status")
    public static class SecurityStatusRequest {
        @Schema(description = "The new security status", example = "RETURNED", required = true)
        @NotNull(message = "Status is required")
        private SecurityStatus status;

        public SecurityStatus getStatus() {
            return status;
        }

        public void setStatus(SecurityStatus status) {
            this.status = status;
        }
    }
}
