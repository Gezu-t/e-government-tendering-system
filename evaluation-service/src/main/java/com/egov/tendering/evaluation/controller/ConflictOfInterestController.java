package com.egov.tendering.evaluation.controller;

import com.egov.tendering.evaluation.dal.dto.ConflictDeclarationRequest;
import com.egov.tendering.evaluation.dal.dto.ConflictOfInterestDTO;
import com.egov.tendering.evaluation.service.ConflictOfInterestService;
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
@RequestMapping("/api/conflict-of-interest")
@RequiredArgsConstructor
@Slf4j
public class ConflictOfInterestController {

    private final ConflictOfInterestService conflictService;

    @PostMapping("/tenders/{tenderId}/declare")
    @PreAuthorize("hasAnyRole('EVALUATOR', 'COMMITTEE')")
    public ResponseEntity<ConflictOfInterestDTO> declareConflict(
            @PathVariable Long tenderId,
            @Valid @RequestBody ConflictDeclarationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long evaluatorId = getUserId(jwt);
        ConflictOfInterestDTO result = conflictService.declareConflict(tenderId, request, evaluatorId);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @GetMapping("/tenders/{tenderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE', 'COMMITTEE')")
    public ResponseEntity<List<ConflictOfInterestDTO>> getDeclarations(@PathVariable Long tenderId) {
        return ResponseEntity.ok(conflictService.getDeclarationsForTender(tenderId));
    }

    @GetMapping("/tenders/{tenderId}/conflicts")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE')")
    public ResponseEntity<List<ConflictOfInterestDTO>> getConflicts(@PathVariable Long tenderId) {
        return ResponseEntity.ok(conflictService.getConflictsForTender(tenderId));
    }

    @GetMapping("/tenders/{tenderId}/evaluators/{evaluatorId}/status")
    public ResponseEntity<Boolean> hasEvaluatorDeclared(
            @PathVariable Long tenderId, @PathVariable Long evaluatorId) {
        return ResponseEntity.ok(conflictService.hasEvaluatorDeclared(tenderId, evaluatorId));
    }

    @PutMapping("/{declarationId}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE')")
    public ResponseEntity<ConflictOfInterestDTO> reviewDeclaration(
            @PathVariable Long declarationId,
            @RequestParam String decision,
            @RequestParam(required = false) String comments,
            @AuthenticationPrincipal Jwt jwt) {
        Long reviewerId = getUserId(jwt);
        ConflictOfInterestDTO result = conflictService.reviewDeclaration(declarationId, decision, comments, reviewerId);
        return ResponseEntity.ok(result);
    }

    private Long getUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim instanceof Number number) return number.longValue();
        if (userIdClaim != null) return Long.parseLong(userIdClaim.toString());
        return Long.parseLong(jwt.getSubject());
    }
}
