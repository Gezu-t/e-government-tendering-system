package com.egov.tendering.document.controller;

import com.egov.tendering.document.dal.dto.DocumentResponse;
import com.egov.tendering.document.dal.dto.*;
import com.egov.tendering.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * REST controller for document management
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Management", description = "API for document management")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new document")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestPart("metadata") @Valid DocumentUploadRequest metadata,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("REST request to upload document: {}", metadata.getName());
        Set<String> userIdentifiers = getUserIdentifiers(jwt);
        DocumentResponse response = documentService.uploadDocument(metadata, file, primaryUserIdentifier(jwt, userIdentifiers));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document metadata by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("REST request to get document: {}", id);
        DocumentResponse response = documentService.getDocument(id, getUserIdentifiers(jwt), isAdmin(jwt));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download a document")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        log.info("REST request to download document: {}", id);
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        DocumentDownloadResponse downloadResponse = documentService.downloadDocument(
                id, getUserIdentifiers(jwt), isAdmin(jwt), ipAddress, userAgent);

        ByteArrayResource resource = new ByteArrayResource(downloadResponse.getContent());

        String encodedFilename = URLEncoder.encode(downloadResponse.getOriginalFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloadResponse.getContentType()))
                .contentLength(downloadResponse.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update document metadata")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long id,
            @RequestBody @Valid DocumentUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("REST request to update document: {}", id);
        DocumentResponse response = documentService.updateDocument(id, request, getUserIdentifiers(jwt), isAdmin(jwt));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("REST request to delete document: {}", id);
        documentService.deleteDocument(id, getUserIdentifiers(jwt), isAdmin(jwt));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "List documents for an entity")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<DocumentResponse>> listDocumentsByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("REST request to list documents for entity: {}, ID: {}", entityType, entityId);
        Page<DocumentResponse> response = documentService.listDocumentsByEntity(
                entityType, entityId, getUserIdentifiers(jwt), isAdmin(jwt), pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    @Operation(summary = "Search documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<DocumentResponse>> searchDocuments(
            @RequestBody DocumentSearchFilter filter,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("REST request to search documents with filter: {}", filter);
        Page<DocumentResponse> response = documentService.searchDocuments(filter, getUserIdentifiers(jwt), isAdmin(jwt), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/access-logs")
    @Operation(summary = "Get document access logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DocumentAccessLogResponse>> getDocumentAccessLogs(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("REST request to get access logs for document: {}", id);
        Page<DocumentAccessLogResponse> response = documentService.getDocumentAccessLogs(id, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // In case of multiple proxies, the first IP is the client IP
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isAdmin(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        Object rolesClaim = jwt.getClaims().get("roles");
        if (rolesClaim instanceof java.util.Collection<?> roles) {
            return roles.stream().anyMatch(role -> "ROLE_ADMIN".equals(String.valueOf(role)));
        }
        return rolesClaim != null && rolesClaim.toString().contains("ROLE_ADMIN");
    }

    private Set<String> getUserIdentifiers(Jwt jwt) {
        Set<String> identifiers = new LinkedHashSet<>();
        if (jwt == null) {
            return identifiers;
        }
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim != null && !userIdClaim.toString().isBlank()) {
            identifiers.add(userIdClaim.toString());
        }
        if (jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
            identifiers.add(jwt.getSubject());
        }
        return identifiers;
    }

    private String primaryUserIdentifier(Jwt jwt, Set<String> userIdentifiers) {
        if (jwt != null) {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim != null && !userIdClaim.toString().isBlank()) {
                return userIdClaim.toString();
            }
        }
        return userIdentifiers.stream().findFirst().orElse("unknown");
    }
}
