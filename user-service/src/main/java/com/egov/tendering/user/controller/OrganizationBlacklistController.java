package com.egov.tendering.user.controller;

import com.egov.tendering.user.dal.dto.BlacklistRequest;
import com.egov.tendering.user.dal.dto.OrganizationBlacklistDTO;
import com.egov.tendering.user.service.OrganizationBlacklistService;
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
@RequestMapping("/api/blacklist")
@RequiredArgsConstructor
@Slf4j
public class OrganizationBlacklistController {

    private final OrganizationBlacklistService blacklistService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizationBlacklistDTO> blacklistOrganization(
            @Valid @RequestBody BlacklistRequest request,
            @AuthenticationPrincipal UserDetails user) {
        Long imposedBy = Long.parseLong(user.getUsername());
        OrganizationBlacklistDTO result = blacklistService.blacklistOrganization(request, imposedBy);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PutMapping("/{blacklistId}/lift")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizationBlacklistDTO> liftBlacklist(
            @PathVariable Long blacklistId,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails user) {
        Long liftedBy = Long.parseLong(user.getUsername());
        OrganizationBlacklistDTO result = blacklistService.liftBlacklist(blacklistId, reason, liftedBy);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/organization/{organizationId}/status")
    public ResponseEntity<Boolean> isBlacklisted(@PathVariable Long organizationId) {
        return ResponseEntity.ok(blacklistService.isOrganizationBlacklisted(organizationId));
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<OrganizationBlacklistDTO>> getBlacklists(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(blacklistService.getBlacklistsForOrganization(organizationId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TENDEREE')")
    public ResponseEntity<Page<OrganizationBlacklistDTO>> getActiveBlacklists(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(blacklistService.getActiveBlacklists(pageable));
    }
}
