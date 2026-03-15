package com.egov.tendering.audit.dal.repository;

import com.egov.tendering.audit.dal.model.AuditActionType;
import com.egov.tendering.audit.dal.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entities
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /**
     * Find audit logs by user ID
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    /**
     * Find audit logs by action type
     */
    Page<AuditLog> findByActionTypeOrderByTimestampDesc(AuditActionType actionType, Pageable pageable);

    /**
     * Find audit logs by entity type and entity ID
     */
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId, Pageable pageable);

    /**
     * Find audit logs by timestamp range
     */
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find audit logs by user ID and timestamp range
     */
    Page<AuditLog> findByUserIdAndTimestampBetweenOrderByTimestampDesc(
            Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find audit logs by entity type, entity ID, and timestamp range
     */
    Page<AuditLog> findByEntityTypeAndEntityIdAndTimestampBetweenOrderByTimestampDesc(
            String entityType, String entityId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find audit logs by action type and timestamp range
     */
    Page<AuditLog> findByActionTypeAndTimestampBetweenOrderByTimestampDesc(
            AuditActionType actionType, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find all failure audit logs
     */
    Page<AuditLog> findBySuccessFalseOrderByTimestampDesc(Pageable pageable);

    /**
     * Count audit logs by action type and timestamp range
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.actionType = :actionType AND a.timestamp BETWEEN :startTime AND :endTime")
    long countByActionTypeAndTimestampBetween(
            @Param("actionType") AuditActionType actionType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get audit action summary by action type grouped by day
     */
    @Query("SELECT DATE(a.timestamp) as date, COUNT(a) as count FROM AuditLog a " +
            "WHERE a.actionType = :actionType AND a.timestamp BETWEEN :startTime AND :endTime " +
            "GROUP BY DATE(a.timestamp) ORDER BY date ASC")
    List<Object[]> getActionSummaryByDay(
            @Param("actionType") AuditActionType actionType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find audit logs by correlation ID
     */
    List<AuditLog> findByCorrelationIdOrderByTimestampAsc(String correlationId);

    /**
     * Search audit logs by keyword in details
     */
    @Query("SELECT a FROM AuditLog a WHERE a.details LIKE %:keyword%")
    Page<AuditLog> searchByDetailsContaining(@Param("keyword") String keyword, Pageable pageable);
}
