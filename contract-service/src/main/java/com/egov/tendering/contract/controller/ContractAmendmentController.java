package com.egov.tendering.contract.controller;

import com.egov.tendering.contract.dal.dto.ContractAmendmentDTO;
import com.egov.tendering.contract.dal.dto.ContractAmendmentRequest;
import com.egov.tendering.contract.service.ContractAmendmentService;
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
@RequestMapping("/api/contracts/{contractId}/amendments")
@RequiredArgsConstructor
@Slf4j
public class ContractAmendmentController {

    private final ContractAmendmentService amendmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE')")
    public ResponseEntity<ContractAmendmentDTO> requestAmendment(
            @PathVariable Long contractId,
            @Valid @RequestBody ContractAmendmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        ContractAmendmentDTO result = amendmentService.requestAmendment(contractId, request, userId);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PutMapping("/{amendmentId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContractAmendmentDTO> approveAmendment(
            @PathVariable Long contractId,
            @PathVariable Long amendmentId,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(amendmentService.approveAmendment(amendmentId, userId));
    }

    @PutMapping("/{amendmentId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContractAmendmentDTO> rejectAmendment(
            @PathVariable Long contractId,
            @PathVariable Long amendmentId,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(amendmentService.rejectAmendment(amendmentId, userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE', 'TENDERER')")
    public ResponseEntity<List<ContractAmendmentDTO>> getAmendments(@PathVariable Long contractId) {
        return ResponseEntity.ok(amendmentService.getAmendmentsByContract(contractId));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE')")
    public ResponseEntity<List<ContractAmendmentDTO>> getPending(@PathVariable Long contractId) {
        return ResponseEntity.ok(amendmentService.getPendingAmendments(contractId));
    }

    private Long getUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim instanceof Number number) return number.longValue();
        if (userIdClaim != null) return Long.parseLong(userIdClaim.toString());
        return Long.parseLong(jwt.getSubject());
    }
}
