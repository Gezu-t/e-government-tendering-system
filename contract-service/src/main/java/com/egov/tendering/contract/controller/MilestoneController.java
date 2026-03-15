package com.egov.tendering.contract.controller;

import com.egov.tendering.contract.dal.dto.ContractMilestoneDTO;
import com.egov.tendering.contract.service.MilestoneService;
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
@RequestMapping("/api/contracts/{contractId}/milestones")
@RequiredArgsConstructor
@Slf4j
public class MilestoneController {

    private final MilestoneService milestoneService;

    @GetMapping
    @PreAuthorize("@contractSecurityUtil.canAccessContract(#contractId)")
    public ResponseEntity<List<ContractMilestoneDTO>> getMilestonesByContractId(@PathVariable Long contractId) {
        log.info("Received request to get milestones for contract ID: {}", contractId);
        List<ContractMilestoneDTO> milestones = milestoneService.getMilestonesByContractId(contractId);
        return ResponseEntity.ok(milestones);
    }

    @PostMapping
    @PreAuthorize("@contractSecurityUtil.canManageContract(#contractId)")
    public ResponseEntity<ContractMilestoneDTO> addMilestone(
            @PathVariable Long contractId,
            @Valid @RequestBody ContractMilestoneDTO milestoneDTO,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();

        log.info("Received request to add milestone to contract ID: {}", contractId);
        ContractMilestoneDTO addedMilestone = milestoneService.addMilestone(contractId, milestoneDTO, username);
        return new ResponseEntity<>(addedMilestone, HttpStatus.CREATED);
    }

    @PatchMapping("/{milestoneId}/complete")
    @PreAuthorize("@contractSecurityUtil.canManageMilestone(#contractId, #milestoneId)")
    public ResponseEntity<ContractMilestoneDTO> completeMilestone(
            @PathVariable Long contractId,
            @PathVariable Long milestoneId,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();

        log.info("Received request to complete milestone ID: {} for contract ID: {}", milestoneId, contractId);
        ContractMilestoneDTO completedMilestone = milestoneService.completeMilestone(contractId, milestoneId, username);
        return ResponseEntity.ok(completedMilestone);
    }
}
