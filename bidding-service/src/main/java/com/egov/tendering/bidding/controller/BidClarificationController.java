package com.egov.tendering.bidding.controller;

import com.egov.tendering.bidding.config.JwtUserIdExtractor;

import com.egov.tendering.bidding.dal.dto.BidClarificationDTO;
import com.egov.tendering.bidding.service.BidClarificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/bids")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bid Clarifications", description = "APIs for managing bid clarifications")
public class BidClarificationController {

    private final BidClarificationService clarificationService;
    private final JwtUserIdExtractor jwtUserIdExtractor;

    @GetMapping("/{bidId}/clarifications")
    @Operation(summary = "Get all clarifications for a bid",
            description = "Retrieves all clarification requests and responses for the specified bid")
    @ApiResponse(responseCode = "200", description = "List of clarifications retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Bid not found")
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN') or @bidAccessSecurityUtil.isBidOwner(#bidId)")
    public ResponseEntity<List<BidClarificationDTO>> getClarificationsByBidId(
            @PathVariable @Parameter(description = "ID of the bid") Long bidId) {
        log.info("REST request to get clarifications for bid ID: {}", bidId);
        List<BidClarificationDTO> clarifications = clarificationService.getClarificationsByBidId(bidId);
        return ResponseEntity.ok(clarifications);
    }

    @PostMapping("/{bidId}/clarifications")
    @Operation(summary = "Request a clarification on a bid",
            description = "Create a new clarification request for a bid that requires additional information")
    @ApiResponse(responseCode = "201", description = "Clarification request created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "404", description = "Bid not found")
    @PreAuthorize("hasRole('EVALUATOR')")
    public ResponseEntity<BidClarificationDTO> requestClarification(
            @PathVariable @Parameter(description = "ID of the bid") Long bidId,
            @RequestBody @Valid ClarificationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long evaluatorId = getUserId(jwt);
        log.info("REST request to create clarification for bid ID: {} by evaluator ID: {}", bidId, evaluatorId);

        BidClarificationDTO result = clarificationService.requestClarification(
                bidId,
                request.getQuestion(),
                evaluatorId,
                request.getDaysToRespond());

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/clarifications/{clarificationId}/respond")
    @Operation(summary = "Respond to a clarification request",
            description = "Submit a response to a pending clarification request")
    @ApiResponse(responseCode = "200", description = "Response submitted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid state or expired deadline")
    @ApiResponse(responseCode = "403", description = "Not authorized to respond to this clarification")
    @ApiResponse(responseCode = "404", description = "Clarification not found")
    @PreAuthorize("hasRole('TENDERER')")
    public ResponseEntity<BidClarificationDTO> respondToClarification(
            @PathVariable @Parameter(description = "ID of the clarification") Long clarificationId,
            @RequestBody @Valid ClarificationResponse response,
            @AuthenticationPrincipal Jwt jwt) {
        Long tendererId = getUserId(jwt);
        log.info("REST request to respond to clarification ID: {} by tenderer ID: {}", clarificationId, tendererId);

        BidClarificationDTO result = clarificationService.respondToClarification(
                clarificationId,
                response.getResponse(),
                tendererId);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{bidId}/clarifications/expire")
    @Operation(summary = "Expire pending clarifications",
            description = "Marks all overdue clarifications for a bid as expired")
    @ApiResponse(responseCode = "204", description = "Clarifications expired successfully")
    @ApiResponse(responseCode = "404", description = "Bid not found")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> expireClarifications(
            @PathVariable @Parameter(description = "ID of the bid") Long bidId) {
        log.info("REST request to expire clarifications for bid ID: {}", bidId);
        clarificationService.expireClarifications(bidId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Jwt jwt) {
        return jwtUserIdExtractor.requireUserId(jwt);
    }

    // Request/Response DTOs
    @Schema(description = "Request object for creating a clarification request")
    public static class ClarificationRequest {
        @Schema(description = "The clarification question", example = "Please provide more details about your implementation approach")
        @NotBlank(message = "Question is required")
        private String question;

        @Schema(description = "Number of days given to respond", example = "5")
        @NotNull(message = "Days to respond is required")
        private int daysToRespond;

        // Getters and setters
        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public int getDaysToRespond() {
            return daysToRespond;
        }

        public void setDaysToRespond(int daysToRespond) {
            this.daysToRespond = daysToRespond;
        }
    }

    @Schema(description = "Response object for answering a clarification request")
    public static class ClarificationResponse {
        @Schema(description = "The clarification answer", example = "The implementation will use Spring Batch for processing large datasets")
        @NotBlank(message = "Response is required")
        private String response;

        // Getters and setters
        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }
    }
}
