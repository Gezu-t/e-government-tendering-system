package com.egov.tendering.document.service;

import com.egov.tendering.document.dal.dto.*;
import com.egov.tendering.document.dal.mapper.DocumentMapper;
import com.egov.tendering.document.dal.model.AccessType;
import com.egov.tendering.document.dal.model.Document;
import com.egov.tendering.document.dal.model.DocumentAccessLog;
import com.egov.tendering.document.dal.model.DocumentStatus;
import com.egov.tendering.document.dal.repository.DocumentAccessLogRepository;
import com.egov.tendering.document.dal.repository.DocumentRepository;
import com.egov.tendering.document.event.DocumentEventPublisher;
import com.egov.tendering.document.exception.DocumentAccessDeniedException;
import com.egov.tendering.document.exception.DocumentNotFoundException;
import com.egov.tendering.document.exception.DocumentStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing documents
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentAccessLogRepository accessLogRepository;
    private final DocumentMapper documentMapper;
    private final DocumentEventPublisher eventPublisher;

    @Value("${document.storage.location}")
    private String storageLocation;

    @Value("${document.base.url}")
    private String baseUrl;

    /**
     * Upload a new document
     */
    @Transactional
    public DocumentResponse uploadDocument(DocumentUploadRequest request, MultipartFile file, String userId) {
        log.info("Uploading document: {}", request.getName());

        try {
            // Create storage path
            String filename = generateUniqueFilename(file.getOriginalFilename());
            String storagePath = createStoragePath(filename);

            // Save file
            Path targetLocation = Paths.get(storageLocation).resolve(storagePath);
            Files.createDirectories(targetLocation.getParent());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Calculate checksum
            String checksum = calculateChecksum(file);

            // Create document entity
            Document document = Document.builder()
                    .name(request.getName())
                    .originalFilename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileExtension(getFileExtension(file.getOriginalFilename()))
                    .fileSize(file.getSize())
                    .storagePath(storagePath)
                    .documentType(request.getDocumentType())
                    .entityId(request.getEntityId())
                    .entityType(request.getEntityType())
                    .status(DocumentStatus.UPLOADED)
                    .checksum(checksum)
                    .createdBy(userId)
                    .description(request.getDescription())
                    .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                    .expiresAt(request.getExpiresAt())
                    .version("1.0")
                    .build();

            Document savedDocument = documentRepository.save(document);

            // Publish document uploaded event
            eventPublisher.publishDocumentUploadedEvent(savedDocument);

            // Create response
            DocumentResponse response = documentMapper.toDocumentResponse(savedDocument);
            response.setDownloadUrl(generateDownloadUrl(savedDocument.getId()));

            return response;
        } catch (IOException e) {
            log.error("Failed to upload document", e);
            throw new DocumentStorageException("Failed to upload document: " + e.getMessage());
        }
    }

    /**
     * Get document by ID
     */
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long id, Collection<String> userIdentifiers, boolean admin) {
        log.info("Getting document with ID: {}", id);

        Document document = findDocumentById(id);
        validateReadAccess(document, userIdentifiers, admin);
        DocumentResponse response = documentMapper.toDocumentResponse(document);
        response.setDownloadUrl(generateDownloadUrl(document.getId()));

        return response;
    }

    /**
     * Download a document
     */
    @Transactional
    public DocumentDownloadResponse downloadDocument(Long id, Collection<String> userIdentifiers, boolean admin, String ipAddress, String userAgent) {
        log.info("Downloading document with ID: {}", id);

        Document document = findDocumentById(id);
        validateReadAccess(document, userIdentifiers, admin);

        String auditUserId = firstIdentifier(userIdentifiers);

        // Log the download
        DocumentAccessLog accessLog = DocumentAccessLog.builder()
                .documentId(document.getId())
                .userId(auditUserId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .accessType(AccessType.DOWNLOAD)
                .build();

        accessLogRepository.save(accessLog);

        // Get file content
        byte[] content;
        try {
            Path filePath = Paths.get(storageLocation).resolve(document.getStoragePath());
            content = Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read document file", e);
            throw new DocumentStorageException("Failed to read document file: " + e.getMessage());
        }

        // Publish document downloaded event
        eventPublisher.publishDocumentDownloadedEvent(document, auditUserId);

        return DocumentDownloadResponse.builder()
                .documentId(document.getId())
                .name(document.getName())
                .originalFilename(document.getOriginalFilename())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .content(content)
                .build();
    }

    /**
     * Update document metadata
     */
    @Transactional
    public DocumentResponse updateDocument(Long id, DocumentUpdateRequest request, Collection<String> userIdentifiers, boolean admin) {
        log.info("Updating document with ID: {}", id);

        Document document = findDocumentById(id);
        validateWriteAccess(document, userIdentifiers, admin);

        // Update fields if provided
        if (request.getName() != null) {
            document.setName(request.getName());
        }

        if (request.getDescription() != null) {
            document.setDescription(request.getDescription());
        }

        if (request.getIsPublic() != null) {
            document.setIsPublic(request.getIsPublic());
        }

        if (request.getStatus() != null) {
            document.setStatus(request.getStatus());
        }

        if (request.getExpiresAt() != null) {
            document.setExpiresAt(request.getExpiresAt());
        }

        Document updatedDocument = documentRepository.save(document);

        // Publish document updated event
        eventPublisher.publishDocumentUpdatedEvent(updatedDocument, firstIdentifier(userIdentifiers));

        DocumentResponse response = documentMapper.toDocumentResponse(updatedDocument);
        response.setDownloadUrl(generateDownloadUrl(updatedDocument.getId()));

        return response;
    }

    /**
     * Delete a document
     */
    @Transactional
    public void deleteDocument(Long id, Collection<String> userIdentifiers, boolean admin) {
        log.info("Deleting document with ID: {}", id);

        Document document = findDocumentById(id);
        validateWriteAccess(document, userIdentifiers, admin);
        document.setStatus(DocumentStatus.DELETED);
        documentRepository.save(document);

        // Publish document deleted event
        eventPublisher.publishDocumentDeletedEvent(document, firstIdentifier(userIdentifiers));
    }

    /**
     * List documents for an entity
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocumentsByEntity(String entityType, String entityId, Collection<String> userIdentifiers, boolean admin, Pageable pageable) {
        log.info("Listing documents for entity: {}, ID: {}", entityType, entityId);

        Page<Document> documents;
        if (admin) {
            documents = documentRepository.findByEntityTypeAndEntityIdAndStatusNot(
                    com.egov.tendering.document.dal.model.EntityType.valueOf(entityType),
                    entityId,
                    DocumentStatus.DELETED,
                    pageable
            );
        } else {
            documents = documentRepository.findAccessibleByEntityTypeAndEntityIdAndStatusNot(
                    com.egov.tendering.document.dal.model.EntityType.valueOf(entityType),
                    entityId,
                    normalizeIdentifiers(userIdentifiers),
                    DocumentStatus.DELETED,
                    pageable
            );
        }

        return documents.map(document -> {
            DocumentResponse response = documentMapper.toDocumentResponse(document);
            response.setDownloadUrl(generateDownloadUrl(document.getId()));
            return response;
        });
    }

    /**
     * Search documents
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> searchDocuments(DocumentSearchFilter filter, Collection<String> userIdentifiers, boolean admin, Pageable pageable) {
        log.info("Searching documents with filter: {}", filter);

        String searchTerm = filter.getSearchTerm() != null ? filter.getSearchTerm() : "";
        Page<Document> documents;
        if (admin) {
            documents = documentRepository.findByNameContainingIgnoreCaseAndStatusNot(
                    searchTerm,
                    DocumentStatus.DELETED,
                    pageable
            );
        } else {
            documents = documentRepository.findAccessibleByNameContainingIgnoreCaseAndStatusNot(
                    searchTerm,
                    normalizeIdentifiers(userIdentifiers),
                    DocumentStatus.DELETED,
                    pageable
            );
        }

        return documents.map(document -> {
            DocumentResponse response = documentMapper.toDocumentResponse(document);
            response.setDownloadUrl(generateDownloadUrl(document.getId()));
            return response;
        });
    }

    /**
     * Get document access logs
     */
    @Transactional(readOnly = true)
    public Page<DocumentAccessLogResponse> getDocumentAccessLogs(Long documentId, Pageable pageable) {
        log.info("Getting access logs for document with ID: {}", documentId);

        // Check if document exists
        findDocumentById(documentId);

        Page<DocumentAccessLog> logs = accessLogRepository.findByDocumentIdOrderByAccessedAtDesc(documentId, pageable);

        return logs.map(documentMapper::toDocumentAccessLogResponse);
    }

    /**
     * Process expired documents
     */
    @Transactional
    public void processExpiredDocuments() {
        log.info("Processing expired documents");

        LocalDateTime now = LocalDateTime.now();
        List<Document> expiredDocuments = documentRepository.findByExpiresAtBeforeAndStatusNot(
                now,
                DocumentStatus.DELETED
        );

        for (Document document : expiredDocuments) {
            document.setStatus(DocumentStatus.EXPIRED);
            documentRepository.save(document);

            // Publish document expired event
            eventPublisher.publishDocumentExpiredEvent(document);
        }

        log.info("Processed {} expired documents", expiredDocuments.size());
    }

    /**
     * Find document by ID or throw exception
     */
    private Document findDocumentById(Long id) {
        return documentRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + id));
    }

    private void validateReadAccess(Document document, Collection<String> userIdentifiers, boolean admin) {
        if (admin) {
            return;
        }
        boolean owner = normalizeIdentifiers(userIdentifiers).contains(document.getCreatedBy());
        boolean isPublic = Boolean.TRUE.equals(document.getIsPublic());
        if (!owner && !isPublic) {
            throw new DocumentAccessDeniedException("Access denied to document: " + document.getId());
        }
    }

    private void validateWriteAccess(Document document, Collection<String> userIdentifiers, boolean admin) {
        if (admin) {
            return;
        }
        if (!normalizeIdentifiers(userIdentifiers).contains(document.getCreatedBy())) {
            throw new DocumentAccessDeniedException("Access denied to document: " + document.getId());
        }
    }

    private Set<String> normalizeIdentifiers(Collection<String> userIdentifiers) {
        Set<String> identifiers = new LinkedHashSet<>();
        if (userIdentifiers == null) {
            return identifiers;
        }
        for (String identifier : userIdentifiers) {
            if (identifier != null && !identifier.isBlank()) {
                identifiers.add(identifier);
            }
        }
        return identifiers;
    }

    private String firstIdentifier(Collection<String> userIdentifiers) {
        return normalizeIdentifiers(userIdentifiers).stream().findFirst().orElse("unknown");
    }

    /**
     * Generate a unique filename
     */
    private String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + extension;
    }

    /**
     * Create a storage path for the file
     */
    private String createStoragePath(String filename) {
        // Organize files in subdirectories based on the first characters of the filename
        // to prevent too many files in a single directory
        return filename.substring(0, 2) + "/" + filename.substring(2, 4) + "/" + filename;
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf(".") + 1))
                .orElse("");
    }

    /**
     * Calculate checksum for a file
     */
    private String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(file.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Failed to calculate checksum", e);
            return "";
        }
    }

    /**
     * Generate download URL for a document
     */
    private String generateDownloadUrl(Long documentId) {
        return baseUrl + "/api/v1/documents/" + documentId + "/download";
    }
}
