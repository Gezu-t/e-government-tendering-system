package com.egov.tendering.user.controller;


import com.egov.tendering.user.dal.dto.OrganizationDTO;
import com.egov.tendering.user.dal.dto.OrganizationRequest;
import com.egov.tendering.user.dal.model.OrganizationStatus;
import com.egov.tendering.user.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;
    private final com.egov.tendering.user.security.UserSecurityUtil userSecurityUtil;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationDTO> createOrganization(
            @Valid @RequestBody OrganizationRequest request) {
        Long creatorUserId = userSecurityUtil.getCurrentUserId();
        log.info("Creating organization: {} for user ID: {}", request.getName(), creatorUserId);
        OrganizationDTO createdOrg = organizationService.createOrganization(request, creatorUserId);
        return new ResponseEntity<>(createdOrg, HttpStatus.CREATED);
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationDTO> getOrganizationById(@PathVariable Long organizationId) {
        log.info("Fetching organization by ID: {}", organizationId);
        OrganizationDTO organization = organizationService.getOrganizationById(organizationId);
        return ResponseEntity.ok(organization);
    }

    @GetMapping("/registration/{registrationNumber}")
    public ResponseEntity<OrganizationDTO> getOrganizationByRegistrationNumber(
            @PathVariable String registrationNumber) {
        log.info("Fetching organization by registration number: {}", registrationNumber);
        OrganizationDTO organization = organizationService.getOrganizationByRegistrationNumber(registrationNumber);
        return ResponseEntity.ok(organization);
    }

    @GetMapping
    public ResponseEntity<Page<OrganizationDTO>> getAllOrganizations(
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("Fetching all organizations with pagination");
        Page<OrganizationDTO> organizations = organizationService.getAllOrganizations(pageable);
        return ResponseEntity.ok(organizations);
    }

    @PatchMapping("/{organizationId}/status")
    @PreAuthorize("@userSecurityUtil.canManageOrganization(#organizationId)")
    public ResponseEntity<OrganizationDTO> updateOrganizationStatus(
            @PathVariable Long organizationId,
            @RequestParam OrganizationStatus status) {
        log.info("Updating status to {} for organization ID: {}", status, organizationId);
        OrganizationDTO updatedOrg = organizationService.updateOrganizationStatus(organizationId, status);
        return ResponseEntity.ok(updatedOrg);
    }

    @DeleteMapping("/{organizationId}")
    @PreAuthorize("@userSecurityUtil.canManageOrganization(#organizationId)")
    public ResponseEntity<Void> deleteOrganization(@PathVariable Long organizationId) {
        log.info("Deleting organization with ID: {}", organizationId);
        organizationService.deleteOrganization(organizationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{organizationId}/users/{userId}")
    @PreAuthorize("@userSecurityUtil.canManageOrganization(#organizationId)")
    public ResponseEntity<Void> addUserToOrganization(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @RequestParam String role) {
        log.info("Adding user ID: {} to organization ID: {} with role: {}", userId, organizationId, role);
        organizationService.addUserToOrganization(userId, organizationId, role);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{organizationId}/users/{userId}")
    @PreAuthorize("@userSecurityUtil.canManageOrganization(#organizationId)")
    public ResponseEntity<Void> removeUserFromOrganization(
            @PathVariable Long organizationId,
            @PathVariable Long userId) {
        log.info("Removing user ID: {} from organization ID: {}", userId, organizationId);
        organizationService.removeUserFromOrganization(userId, organizationId);
        return ResponseEntity.noContent().build();
    }
}
