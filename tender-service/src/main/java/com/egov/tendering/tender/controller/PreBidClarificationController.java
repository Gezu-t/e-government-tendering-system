package com.egov.tendering.tender.controller;

import com.egov.tendering.tender.config.JwtUserIdExtractor;
import com.egov.tendering.tender.dal.dto.PreBidAnswerRequest;
import com.egov.tendering.tender.dal.dto.PreBidClarificationDTO;
import com.egov.tendering.tender.dal.dto.PreBidQuestionRequest;
import com.egov.tendering.tender.service.PreBidClarificationService;
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
@RequestMapping("/api/tenders/{tenderId}/clarifications")
@RequiredArgsConstructor
@Slf4j
public class PreBidClarificationController {

    private final PreBidClarificationService clarificationService;
    private final JwtUserIdExtractor jwtUserIdExtractor;

    @PostMapping
    @PreAuthorize("hasAnyRole('TENDERER', 'ADMIN')")
    public ResponseEntity<PreBidClarificationDTO> askQuestion(
            @PathVariable Long tenderId,
            @Valid @RequestBody PreBidQuestionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        log.info("User {} asking question for tender {}", userId, tenderId);
        PreBidClarificationDTO result = clarificationService.askQuestion(tenderId, request, userId);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PutMapping("/{clarificationId}/answer")
    @PreAuthorize("@tenderSecurityUtil.canManageClarification(#tenderId, #clarificationId)")
    public ResponseEntity<PreBidClarificationDTO> answerQuestion(
            @PathVariable Long tenderId,
            @PathVariable Long clarificationId,
            @Valid @RequestBody PreBidAnswerRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        log.info("User {} answering clarification {} for tender {}", userId, clarificationId, tenderId);
        PreBidClarificationDTO result = clarificationService.answerQuestion(tenderId, clarificationId, request, userId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{clarificationId}/reject")
    @PreAuthorize("@tenderSecurityUtil.canManageClarification(#tenderId, #clarificationId)")
    public ResponseEntity<PreBidClarificationDTO> rejectQuestion(
            @PathVariable Long tenderId,
            @PathVariable Long clarificationId,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        PreBidClarificationDTO result = clarificationService.rejectQuestion(tenderId, clarificationId, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/public")
    public ResponseEntity<List<PreBidClarificationDTO>> getPublicClarifications(
            @PathVariable Long tenderId) {
        List<PreBidClarificationDTO> results = clarificationService.getPublicClarifications(tenderId);
        return ResponseEntity.ok(results);
    }

    @GetMapping
    @PreAuthorize("@tenderSecurityUtil.canManageTender(#tenderId)")
    public ResponseEntity<List<PreBidClarificationDTO>> getAllClarifications(
            @PathVariable Long tenderId) {
        List<PreBidClarificationDTO> results = clarificationService.getAllClarifications(tenderId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/pending")
    @PreAuthorize("@tenderSecurityUtil.canManageTender(#tenderId)")
    public ResponseEntity<List<PreBidClarificationDTO>> getPendingClarifications(
            @PathVariable Long tenderId) {
        List<PreBidClarificationDTO> results = clarificationService.getPendingClarifications(tenderId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PreBidClarificationDTO>> getMyClarifications(
            @PathVariable Long tenderId,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        List<PreBidClarificationDTO> results = clarificationService.getMyClarifications(tenderId, userId);
        return ResponseEntity.ok(results);
    }

    private Long getUserId(Jwt jwt) {
        return jwtUserIdExtractor.requireUserId(jwt);
    }
}
