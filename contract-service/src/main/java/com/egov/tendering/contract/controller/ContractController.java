package com.egov.tendering.contract.controller;

import com.egov.tendering.contract.dal.dto.ContractDTO;
import com.egov.tendering.contract.dal.dto.CreateContractRequest;
import com.egov.tendering.contract.dal.model.ContractStatus;
import com.egov.tendering.contract.service.ContractService;
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
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Slf4j
public class ContractController {

  private final ContractService contractService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE')")
  public ResponseEntity<ContractDTO> createContract(
          @Valid @RequestBody CreateContractRequest request,
          @AuthenticationPrincipal Jwt jwt) {
    String username = jwt.getSubject();

    log.info("Received request to create contract: {}", request.getTitle());
    ContractDTO createdContract = contractService.createContract(request, username);
    return new ResponseEntity<>(createdContract, HttpStatus.CREATED);
  }

  @GetMapping("/{contractId}")
  @PreAuthorize("@contractSecurityUtil.canAccessContract(#contractId)")
  public ResponseEntity<ContractDTO> getContractById(@PathVariable Long contractId) {
    log.info("Received request to get contract by ID: {}", contractId);
    ContractDTO contract = contractService.getContractById(contractId);
    return ResponseEntity.ok(contract);
  }

  @GetMapping("/tender/{tenderId}")
  @PreAuthorize("@contractSecurityUtil.canAccessTenderContracts(#tenderId)")
  public ResponseEntity<List<ContractDTO>> getContractsByTenderId(@PathVariable Long tenderId) {
    log.info("Received request to get contracts by tender ID: {}", tenderId);
    List<ContractDTO> contracts = contractService.getContractsByTenderId(tenderId);
    return ResponseEntity.ok(contracts);
  }

  @GetMapping("/bidder/{bidderId}")
  @PreAuthorize("@contractSecurityUtil.canAccessBidderContracts(#bidderId)")
  public ResponseEntity<Page<ContractDTO>> getContractsByBidderId(
          @PathVariable Long bidderId,
          @PageableDefault(size = 10) Pageable pageable) {

    log.info("Received request to get contracts by bidder ID: {}", bidderId);
    Page<ContractDTO> contracts = contractService.getContractsByBidderId(bidderId, pageable);
    return ResponseEntity.ok(contracts);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE')")
  public ResponseEntity<Page<ContractDTO>> searchContracts(
          @RequestParam(required = false) String title,
          @RequestParam(required = false) ContractStatus status,
          @RequestParam(required = false) Long tenderId,
          @RequestParam(required = false) Long bidderId,
          @AuthenticationPrincipal Jwt jwt,
          @PageableDefault(size = 10) Pageable pageable) {

    log.info("Received request to search contracts with title: {}, status: {}, tenderId: {}, bidderId: {}",
            title, status, tenderId, bidderId);
    Page<ContractDTO> contracts = contractService.searchContracts(
            title,
            status,
            tenderId,
            bidderId,
            jwt.getSubject(),
            hasRole(jwt, "ROLE_ADMIN"),
            pageable);
    return ResponseEntity.ok(contracts);
  }

  @PatchMapping("/{contractId}/status")
  @PreAuthorize("@contractSecurityUtil.canManageContract(#contractId)")
  public ResponseEntity<ContractDTO> updateContractStatus(
          @PathVariable Long contractId,
          @RequestParam ContractStatus status,
          @AuthenticationPrincipal Jwt jwt) {
    String username = jwt.getSubject();

    log.info("Received request to update contract status to {} for contract ID: {}", status, contractId);
    ContractDTO updatedContract = contractService.updateContractStatus(contractId, status, username);
    return ResponseEntity.ok(updatedContract);
  }

  @PostMapping("/{contractId}/activate")
  @PreAuthorize("@contractSecurityUtil.canManageContract(#contractId)")
  public ResponseEntity<ContractDTO> activateContract(
          @PathVariable Long contractId,
          @AuthenticationPrincipal Jwt jwt) {
    String username = jwt.getSubject();

    log.info("Received request to activate contract with ID: {}", contractId);
    ContractDTO activatedContract = contractService.activateContract(contractId, username);
    return ResponseEntity.ok(activatedContract);
  }

  @PostMapping("/{contractId}/complete")
  @PreAuthorize("@contractSecurityUtil.canManageContract(#contractId)")
  public ResponseEntity<ContractDTO> completeContract(
          @PathVariable Long contractId,
          @AuthenticationPrincipal Jwt jwt) {
    String username = jwt.getSubject();

    log.info("Received request to complete contract with ID: {}", contractId);
    ContractDTO completedContract = contractService.completeContract(contractId, username);
    return ResponseEntity.ok(completedContract);
  }

  @PostMapping("/{contractId}/terminate")
  @PreAuthorize("@contractSecurityUtil.canManageContract(#contractId)")
  public ResponseEntity<ContractDTO> terminateContract(
          @PathVariable Long contractId,
          @RequestParam String reason,
          @AuthenticationPrincipal Jwt jwt) {
    String username = jwt.getSubject();

    log.info("Received request to terminate contract with ID: {}", contractId);
    ContractDTO terminatedContract = contractService.terminateContract(contractId, reason, username);
    return ResponseEntity.ok(terminatedContract);
  }

  private boolean hasRole(Jwt jwt, String role) {
    Object roles = jwt.getClaim("roles");
    if (roles instanceof Iterable<?> iterable) {
      for (Object value : iterable) {
        if (role.equals(String.valueOf(value))) {
          return true;
        }
      }
    }
    return false;
  }
}
