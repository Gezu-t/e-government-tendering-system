package com.egov.tendering.bidding.controller;

import com.egov.tendering.bidding.dal.dto.ComplianceCheckResult;
import com.egov.tendering.bidding.dal.dto.ComplianceRequirementDTO;
import com.egov.tendering.bidding.service.ComplianceService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Compliance", description = "APIs for managing tender compliance requirements and bid compliance checks")
public class ComplianceController {

    private final ComplianceService complianceService;

    @GetMapping("/tenders/{tenderId}/compliance-requirements")
    @Operation(summary = "Get compliance requirements for a tender",
            description = "Retrieves all compliance requirements defined for the specified tender")
    @ApiResponse(responseCode = "200", description = "Requirements retrieved successfully")
    @PreAuthorize("hasAnyRole('EVALUATOR', 'TENDERER', 'ADMIN')")
    public ResponseEntity<List<ComplianceRequirementDTO>> getComplianceRequirements(
            @PathVariable @Parameter(description = "ID of the tender") Long tenderId) {
        log.info("REST request to get compliance requirements for tender ID: {}", tenderId);
        List<ComplianceRequirementDTO> requirements = complianceService.getComplianceRequirements(tenderId);
        return ResponseEntity.ok(requirements);
    }

    @PostMapping("/tenders/{tenderId}/compliance-requirements")
    @Operation(summary = "Add a compliance requirement",
            description = "Creates a new compliance requirement for the specified tender")
    @ApiResponse(responseCode = "201", description = "Requirement created successfully")
    @ApiResponse(responseCode = "404", description = "Tender not found")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplianceRequirementDTO> addComplianceRequirement(
            @PathVariable @Parameter(description = "ID of the tender") Long tenderId,
            @RequestBody @Valid ComplianceRequirementDTO requirementDTO,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        log.info("REST request to add compliance requirement for tender ID: {}", tenderId);

        ComplianceRequirementDTO result = complianceService.addComplianceRequirement(
                tenderId,
                requirementDTO,
                userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/compliance-requirements/{requirementId}")
    @Operation(summary = "Delete a compliance requirement",
            description = "Deletes the specified compliance requirement if not associated with any bids")
    @ApiResponse(responseCode = "204", description = "Requirement deleted successfully")
    @ApiResponse(responseCode = "400", description = "Requirement is in use and cannot be deleted")
    @ApiResponse(responseCode = "404", description = "Requirement not found")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteComplianceRequirement(
            @PathVariable @Parameter(description = "ID of the requirement") Long requirementId) {
        log.info("REST request to delete compliance requirement ID: {}", requirementId);
        complianceService.deleteComplianceRequirement(requirementId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bids/{bidId}/compliance-check")
    @Operation(summary = "Check bid compliance",
            description = "Performs automated compliance check for a bid against tender requirements")
    @ApiResponse(responseCode = "200", description = "Compliance check completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid bid state")
    @ApiResponse(responseCode = "404", description = "Bid not found")
    @PreAuthorize("hasRole('EVALUATOR')")
    public ResponseEntity<ComplianceCheckResult> checkBidCompliance(
            @PathVariable @Parameter(description = "ID of the bid") Long bidId) {
        log.info("REST request to check compliance for bid ID: {}", bidId);
        ComplianceCheckResult result = complianceService.checkBidCompliance(bidId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/compliance-items/{complianceItemId}/verify")
    @Operation(summary = "Verify a compliance item",
            description = "Manual verification of an automated compliance check result by an evaluator")
    @ApiResponse(responseCode = "204", description = "Compliance item verified successfully")
    @ApiResponse(responseCode = "400", description = "Invalid bid state")
    @ApiResponse(responseCode = "404", description = "Compliance item not found")
    @PreAuthorize("hasRole('EVALUATOR')")
    public ResponseEntity<Void> verifyComplianceItem(
            @PathVariable @Parameter(description = "ID of the compliance item") Long complianceItemId,
            @RequestBody @Valid ComplianceVerificationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long evaluatorId = getUserId(jwt);
        log.info("REST request to verify compliance item ID: {} with status: {}",
                complianceItemId, request.getCompliant() ? "compliant" : "non-compliant");

        complianceService.verifyComplianceItem(
                complianceItemId,
                request.getCompliant(),
                request.getComment(),
                evaluatorId);

        return ResponseEntity.noContent().build();
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

    // Request DTO
    @Schema(description = "Request object for verifying a compliance item")
    public static class ComplianceVerificationRequest {
        @Schema(description = "Whether the item is compliant", example = "true")
        @NotNull(message = "Compliant status is required")
        private Boolean compliant;

        @Schema(description = "Verification comment", example = "Document verified against regulations")
        private String comment;

        public Boolean getCompliant() {
            return compliant;
        }

        public void setCompliant(Boolean compliant) {
            this.compliant = compliant;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}
