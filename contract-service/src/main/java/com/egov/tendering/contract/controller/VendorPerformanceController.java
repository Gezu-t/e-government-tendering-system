package com.egov.tendering.contract.controller;

import com.egov.tendering.contract.dal.dto.VendorPerformanceDTO;
import com.egov.tendering.contract.dal.dto.VendorPerformanceRequest;
import com.egov.tendering.contract.service.VendorPerformanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vendor-performance")
@RequiredArgsConstructor
@Slf4j
public class VendorPerformanceController {

    private final VendorPerformanceService performanceService;

    @PostMapping("/contracts/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE')")
    public ResponseEntity<VendorPerformanceDTO> submitReview(
            @PathVariable Long contractId,
            @Valid @RequestBody VendorPerformanceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long reviewerId = getUserId(jwt);
        VendorPerformanceDTO result = performanceService.submitPerformanceReview(contractId, request, reviewerId);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @GetMapping("/contracts/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE', 'TENDERER')")
    public ResponseEntity<List<VendorPerformanceDTO>> getByContract(@PathVariable Long contractId) {
        return ResponseEntity.ok(performanceService.getPerformancesByContract(contractId));
    }

    @GetMapping("/vendors/{vendorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE', 'TENDERER')")
    public ResponseEntity<List<VendorPerformanceDTO>> getByVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(performanceService.getPerformancesByVendor(vendorId));
    }

    @GetMapping("/vendors/{vendorId}/average")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE', 'TENDERER')")
    public ResponseEntity<BigDecimal> getVendorAverage(@PathVariable Long vendorId) {
        return ResponseEntity.ok(performanceService.getVendorAverageScore(vendorId));
    }

    private Long getUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim instanceof Number number) return number.longValue();
        if (userIdClaim != null) return Long.parseLong(userIdClaim.toString());
        return Long.parseLong(jwt.getSubject());
    }
}
