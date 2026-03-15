package com.egov.tendering.bidding.controller;

import com.egov.tendering.bidding.config.JwtUserIdExtractor;
import com.egov.tendering.bidding.dal.dto.BidDTO;
import com.egov.tendering.bidding.dal.dto.PageDTO;
import com.egov.tendering.bidding.dal.model.BidStatus;
import com.egov.tendering.bidding.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.egov.tendering.bidding.dal.dto.BidSubmissionRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
@Slf4j
public class BidController {

  private final BidService bidService;
  private final JwtUserIdExtractor jwtUserIdExtractor;

  @PostMapping
  @PreAuthorize("hasRole('TENDERER')")
  public ResponseEntity<BidDTO> createBid(
          @Valid @RequestBody BidSubmissionRequest request,
          @AuthenticationPrincipal Jwt jwt) {
    Long tendererId = getUserId(jwt);

    log.info("Creating bid for tender ID: {} by tenderer ID: {}", request.getTenderId(), tendererId);
    BidDTO createdBid = bidService.createBid(request, tendererId);
    return new ResponseEntity<>(createdBid, HttpStatus.CREATED);
  }

  @GetMapping("/{bidId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR') or @bidAccessSecurityUtil.isBidOwner(#bidId)")
  public ResponseEntity<BidDTO> getBidById(@PathVariable Long bidId) {
    log.info("Getting bid by ID: {}", bidId);
    BidDTO bid = bidService.getBidById(bidId);
    return ResponseEntity.ok(bid);
  }

  @PostMapping("/{bidId}/submit")
  @PreAuthorize("hasRole('TENDERER') and @bidAccessSecurityUtil.isBidOwner(#bidId)")
  public ResponseEntity<BidDTO> submitBid(@PathVariable Long bidId) {
    log.info("Submitting bid with ID: {}", bidId);
    BidDTO submittedBid = bidService.submitBid(bidId);
    return ResponseEntity.ok(submittedBid);
  }

  @PatchMapping("/{bidId}/status")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  public ResponseEntity<BidDTO> updateBidStatus(
          @PathVariable Long bidId,
          @RequestParam BidStatus status) {

    log.info("Updating bid status to {} for bid ID: {}", status, bidId);
    BidDTO updatedBid = bidService.updateBidStatus(bidId, status);
    return ResponseEntity.ok(updatedBid);
  }

  @DeleteMapping("/{bidId}")
  @PreAuthorize("hasRole('TENDERER') and @bidAccessSecurityUtil.isBidOwner(#bidId)")
  public ResponseEntity<Void> deleteBid(@PathVariable Long bidId) {
    log.info("Deleting bid with ID: {}", bidId);
    bidService.deleteBid(bidId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/tenderer")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'TENDERER')")
  public ResponseEntity<PageDTO<BidDTO>> getBidsByTenderer(
          @RequestParam(value = "tendererId", required = false) Long requestedTendererId,
          @AuthenticationPrincipal Jwt jwt,
          @PageableDefault(size = 10) Pageable pageable) {
    Long tendererId = resolveTendererId(requestedTendererId, jwt);

    log.info("Getting bids for tenderer ID: {}", tendererId);
    PageDTO<BidDTO> bids = bidService.getBidsByTenderer(tendererId, pageable);
    return ResponseEntity.ok(bids);
  }

  @GetMapping("/tender/{tenderId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  public ResponseEntity<PageDTO<BidDTO>> getBidsByTender(
          @PathVariable Long tenderId,
          @PageableDefault(size = 10) Pageable pageable) {

    log.info("Getting bids for tender ID: {}", tenderId);
    PageDTO<BidDTO> bids = bidService.getBidsByTender(tenderId, pageable);
    return ResponseEntity.ok(bids);
  }

  @GetMapping("/tender/{tenderId}/status/{status}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  public ResponseEntity<List<BidDTO>> getBidsByTenderAndStatus(
          @PathVariable Long tenderId,
          @PathVariable BidStatus status) {

    log.info("Getting bids for tender ID: {} with status: {}", tenderId, status);
    List<BidDTO> bids = bidService.getBidsByTenderAndStatus(tenderId, status);
    return ResponseEntity.ok(bids);
  }

  @PostMapping("/{bidId}/documents")
  @PreAuthorize("hasRole('TENDERER') and @bidAccessSecurityUtil.isBidOwner(#bidId)")
  public ResponseEntity<BidDTO> addDocumentToBid(
          @PathVariable Long bidId,
          @RequestParam("file") MultipartFile file,
          @RequestParam("fileName") String fileName) {

    log.info("Adding document to bid ID: {}", bidId);
    BidDTO updatedBid = bidService.addDocumentToBid(bidId, file, fileName);
    return ResponseEntity.ok(updatedBid);
  }

  @DeleteMapping("/{bidId}/documents/{documentId}")
  @PreAuthorize("hasRole('TENDERER') and @bidAccessSecurityUtil.isBidOwner(#bidId)")
  public ResponseEntity<Void> removeDocumentFromBid(
          @PathVariable Long bidId,
          @PathVariable Long documentId) {

    log.info("Removing document ID: {} from bid ID: {}", documentId, bidId);
    bidService.removeDocumentFromBid(bidId, documentId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/check")
  @PreAuthorize("hasRole('TENDERER')")
  public ResponseEntity<Boolean> hasTendererBidForTender(
          @RequestParam Long tenderId,
          @AuthenticationPrincipal Jwt jwt) {
    Long tendererId = getUserId(jwt);

    log.info("Checking if tenderer ID: {} has bid for tender ID: {}", tendererId, tenderId);
    boolean hasBid = bidService.hasTendererBidForTender(tenderId, tendererId);
    return ResponseEntity.ok(hasBid);
  }

  private Long resolveTendererId(Long requestedTendererId, Jwt jwt) {
    Long authenticatedUserId = getUserId(jwt);
    if (hasRole(jwt, "ROLE_TENDERER")) {
      return authenticatedUserId;
    }
    return requestedTendererId != null ? requestedTendererId : authenticatedUserId;
  }

  private boolean hasRole(Jwt jwt, String role) {
    Object rolesClaim = jwt.getClaim("roles");
    if (rolesClaim instanceof Iterable<?> iterable) {
      for (Object item : iterable) {
        if (role.equals(String.valueOf(item))) {
          return true;
        }
      }
    }
    return false;
  }

  private Long getUserId(Jwt jwt) {
    return jwtUserIdExtractor.requireUserId(jwt);
  }
}
