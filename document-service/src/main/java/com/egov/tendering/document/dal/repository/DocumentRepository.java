
package com.egov.tendering.document.dal.repository;

import com.egov.tendering.document.dal.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing document entities
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Find a document by its ID and check if it's not deleted
     */
    @Query("SELECT d FROM Document d WHERE d.id = :id AND d.status != 'DELETED'")
    Optional<Document> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * Find all documents for a specific entity
     */
    List<Document> findByEntityTypeAndEntityIdAndStatusNot(EntityType entityType, String entityId, DocumentStatus status);

    /**
     * Find all documents for a specific entity with pagination
     */
    Page<Document> findByEntityTypeAndEntityIdAndStatusNot(EntityType entityType, String entityId, DocumentStatus status, Pageable pageable);

    @Query("""
            SELECT d FROM Document d
            WHERE d.entityType = :entityType
              AND d.entityId = :entityId
              AND d.status != :status
              AND (d.createdBy IN :createdByValues OR d.isPublic = true)
            """)
    Page<Document> findAccessibleByEntityTypeAndEntityIdAndStatusNot(
            @Param("entityType") EntityType entityType,
            @Param("entityId") String entityId,
            @Param("createdByValues") java.util.Collection<String> createdByValues,
            @Param("status") DocumentStatus status,
            Pageable pageable);

    /**
     * Find all documents by document type and status
     */
    List<Document> findByDocumentTypeAndStatus(DocumentType documentType, DocumentStatus status);

    /**
     * Find all documents that expire before a specific date
     */
    List<Document> findByExpiresAtBeforeAndStatusNot(LocalDateTime expiryDate, DocumentStatus status);

    /**
     * Find all public documents for a specific entity
     */
    List<Document> findByEntityTypeAndEntityIdAndIsPublicTrueAndStatusNot(EntityType entityType, String entityId, DocumentStatus status);

    /**
     * Find all documents created by a specific user
     */
    List<Document> findByCreatedByAndStatusNot(String createdBy, DocumentStatus status);

    /**
     * Search documents by name containing the search term
     */
    Page<Document> findByNameContainingIgnoreCaseAndStatusNot(String searchTerm, DocumentStatus status, Pageable pageable);

    @Query("""
            SELECT d FROM Document d
            WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
              AND d.status != :status
              AND (d.createdBy IN :createdByValues OR d.isPublic = true)
            """)
    Page<Document> findAccessibleByNameContainingIgnoreCaseAndStatusNot(
            @Param("searchTerm") String searchTerm,
            @Param("createdByValues") java.util.Collection<String> createdByValues,
            @Param("status") DocumentStatus status,
            Pageable pageable);

    /**
     * Count documents by entity type and entity ID
     */
    long countByEntityTypeAndEntityIdAndStatusNot(EntityType entityType, String entityId, DocumentStatus status);

    /**
     * Find documents by content type
     */
    List<Document> findByContentTypeAndStatusNot(String contentType, DocumentStatus status);
}
