package com.egov.tendering.bidding.controller;

import com.egov.tendering.bidding.config.JwtUserIdExtractor;
import com.egov.tendering.bidding.dal.dto.DigitalSignatureDTO;
import com.egov.tendering.bidding.service.DigitalSignatureService;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/signatures")
@RequiredArgsConstructor
@Slf4j
public class DigitalSignatureController {

    private final DigitalSignatureService signatureService;
    private final JwtUserIdExtractor jwtUserIdExtractor;

    @PostMapping("/{entityType}/{entityId}/sign")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DigitalSignatureDTO> signEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        String username = jwt.getClaimAsString("sub");
        log.info("Request to sign {} {} by user {}", entityType, entityId, userId);
        DigitalSignatureDTO signature = signatureService.signEntity(entityType, entityId, userId, username);
        return new ResponseEntity<>(signature, HttpStatus.CREATED);
    }

    @PostMapping("/{signatureId}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'COMMITTEE')")
    public ResponseEntity<DigitalSignatureDTO> verifySignature(
            @PathVariable Long signatureId,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        log.info("Request to verify signature {} by user {}", signatureId, userId);
        DigitalSignatureDTO signature = signatureService.verifySignature(signatureId, userId);
        return ResponseEntity.ok(signature);
    }

    @PostMapping("/{signatureId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<DigitalSignatureDTO> rejectSignature(
            @PathVariable Long signatureId,
            @RequestParam @NotBlank String reason,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        log.info("Request to reject signature {} by user {}", signatureId, userId);
        DigitalSignatureDTO signature = signatureService.rejectSignature(signatureId, userId, reason);
        return ResponseEntity.ok(signature);
    }

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DigitalSignatureDTO>> getSignatures(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        List<DigitalSignatureDTO> signatures = signatureService.getSignaturesForEntity(entityType, entityId);
        return ResponseEntity.ok(signatures);
    }

    @GetMapping("/{entityType}/{entityId}/verified")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> areAllSignaturesVerified(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        boolean verified = signatureService.areAllSignaturesVerified(entityType, entityId);
        return ResponseEntity.ok(verified);
    }

    private Long getUserId(Jwt jwt) {
        return jwtUserIdExtractor.requireUserId(jwt);
    }
}
