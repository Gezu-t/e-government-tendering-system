package com.egov.tendering.user.controller;

import com.egov.tendering.user.dal.dto.QualificationReviewRequest;
import com.egov.tendering.user.dal.dto.VendorQualificationDTO;
import com.egov.tendering.user.dal.dto.VendorQualificationRequest;
import com.egov.tendering.user.dal.model.QualificationStatus;
import com.egov.tendering.user.service.VendorQualificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor-qualifications")
@RequiredArgsConstructor
@Slf4j
public class VendorQualificationController {

    private final VendorQualificationService qualificationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDERER')")
    public ResponseEntity<VendorQualificationDTO> submitQualification(
            @Valid @RequestBody VendorQualificationRequest request) {
        log.info("Request to submit vendor qualification for org: {}", request.getOrganizationId());
        VendorQualificationDTO result = qualificationService.submitQualification(request);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @GetMapping("/{qualificationId}")
    public ResponseEntity<VendorQualificationDTO> getQualification(@PathVariable Long qualificationId) {
        VendorQualificationDTO result = qualificationService.getQualificationById(qualificationId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<VendorQualificationDTO>> getByOrganization(
            @PathVariable Long organizationId) {
        List<VendorQualificationDTO> results = qualificationService.getQualificationsByOrganization(organizationId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'TENDEREE')")
    public ResponseEntity<Page<VendorQualificationDTO>> getByStatus(
            @PathVariable QualificationStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<VendorQualificationDTO> results = qualificationService.getQualificationsByStatus(status, pageable);
        return ResponseEntity.ok(results);
    }

    @PutMapping("/{qualificationId}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<VendorQualificationDTO> reviewQualification(
            @PathVariable Long qualificationId,
            @Valid @RequestBody QualificationReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Extract reviewer ID from the authenticated user
        Long reviewerId = Long.parseLong(userDetails.getUsername());
        log.info("Request to review qualification: {} by reviewer: {}", qualificationId, reviewerId);
        VendorQualificationDTO result = qualificationService.reviewQualification(
                qualificationId, request, reviewerId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/organization/{organizationId}/qualified")
    public ResponseEntity<Boolean> isOrganizationQualified(@PathVariable Long organizationId) {
        boolean qualified = qualificationService.isOrganizationQualified(organizationId);
        return ResponseEntity.ok(qualified);
    }
}
