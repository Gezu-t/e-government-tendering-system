package com.egov.tendering.tender.controller;

import com.egov.tendering.tender.config.JwtUserIdExtractor;
import com.egov.tendering.tender.dal.dto.*;
import com.egov.tendering.tender.dal.model.TenderStatus;
import com.egov.tendering.tender.dal.model.TenderType;
import com.egov.tendering.tender.service.TenderService;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/tenders")
@RequiredArgsConstructor
@Slf4j
public class TenderController {

  private final TenderService tenderService;
  private final JwtUserIdExtractor jwtUserIdExtractor;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE')")
  public ResponseEntity<TenderDTO> createTender(
          @Valid @RequestBody CreateTenderRequest request,
          @AuthenticationPrincipal Jwt jwt) {
    Long tendereeId = getUserId(jwt);

    log.info("Received request to create tender: {}", request.getTitle());
    TenderDTO createdTender = tenderService.createTender(request, tendereeId);
    return new ResponseEntity<>(createdTender, HttpStatus.CREATED);
  }

  @GetMapping("/{tenderId}")
  @PreAuthorize("@tenderSecurityUtil.canAccessTender(#tenderId)")
  public ResponseEntity<TenderDTO> getTenderById(@PathVariable Long tenderId) {
    log.info("Received request to get tender by ID: {}", tenderId);
    TenderDTO tender = tenderService.getTenderById(tenderId);
    return ResponseEntity.ok(tender);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Page<TenderDTO>> searchTenders(
          @RequestParam(required = false) String title,
          @RequestParam(required = false) TenderStatus status,
          @RequestParam(required = false) TenderType type,
          @PageableDefault(size = 10) Pageable pageable) {

    log.info("Received request to search tenders with title: {}, status: {}, type: {}", title, status, type);
    Page<TenderDTO> tenders = tenderService.searchTenders(title, status, type, pageable);
    return ResponseEntity.ok(tenders);
  }

  @GetMapping("/tenderee")
  @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE')")
  public ResponseEntity<Page<TenderDTO>> getTendersByTenderee(
          @AuthenticationPrincipal Jwt jwt,
          @PageableDefault(size = 10) Pageable pageable) {
    Long tendereeId = getUserId(jwt);

    log.info("Received request to get tenders by tenderee ID: {}", tendereeId);
    Page<TenderDTO> tenders = tenderService.getTendersByTenderee(tendereeId, pageable);
    return ResponseEntity.ok(tenders);
  }

  @PatchMapping("/{tenderId}/status")
  @PreAuthorize("@tenderSecurityUtil.canManageTender(#tenderId)")
  public ResponseEntity<TenderDTO> updateTenderStatus(
          @PathVariable Long tenderId,
          @Valid @RequestBody UpdateTenderStatusRequest request) {

    log.info("Received request to update tender status: {} for tender ID: {}", request.getStatus(), tenderId);
    TenderDTO updatedTender = tenderService.updateTenderStatus(tenderId, request);
    return ResponseEntity.ok(updatedTender);
  }

  @PostMapping("/{tenderId}/publish")
  @PreAuthorize("@tenderSecurityUtil.canManageTender(#tenderId)")
  public ResponseEntity<TenderDTO> publishTender(@PathVariable Long tenderId) {
    log.info("Received request to publish tender with ID: {}", tenderId);
    TenderDTO publishedTender = tenderService.publishTender(tenderId);
    return ResponseEntity.ok(publishedTender);
  }

  @PostMapping("/{tenderId}/close")
  @PreAuthorize("@tenderSecurityUtil.canManageTender(#tenderId)")
  public ResponseEntity<TenderDTO> closeTender(@PathVariable Long tenderId) {
    log.info("Received request to close tender with ID: {}", tenderId);
    TenderDTO closedTender = tenderService.closeTender(tenderId);
    return ResponseEntity.ok(closedTender);
  }

  @PostMapping("/{tenderId}/amend")
  @PreAuthorize("@tenderSecurityUtil.canManageTender(#tenderId)")
  public ResponseEntity<TenderDTO> amendTender(
          @PathVariable Long tenderId,
          @Valid @RequestBody TenderAmendmentRequest request,
          @AuthenticationPrincipal Jwt jwt) {
    Long userId = getUserId(jwt);
    log.info("Received request to amend tender: {} by user: {}", tenderId, userId);
    TenderDTO amendedTender = tenderService.amendTender(tenderId, request, userId);
    return ResponseEntity.ok(amendedTender);
  }

  @GetMapping("/{tenderId}/amendments")
  @PreAuthorize("@tenderSecurityUtil.canAccessTender(#tenderId)")
  public ResponseEntity<List<TenderAmendmentDTO>> getTenderAmendments(@PathVariable Long tenderId) {
    log.info("Received request to get amendments for tender: {}", tenderId);
    List<TenderAmendmentDTO> amendments = tenderService.getTenderAmendments(tenderId);
    return ResponseEntity.ok(amendments);
  }

  private Long getUserId(Jwt jwt) {
    return jwtUserIdExtractor.requireUserId(jwt);
  }
}
