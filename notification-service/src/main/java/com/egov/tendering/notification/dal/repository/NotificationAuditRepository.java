package com.egov.tendering.notification.dal.repository;

import com.egov.tendering.notification.dal.model.NotificationAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing notification audit records
 */
@Repository
public interface NotificationAuditRepository extends JpaRepository<NotificationAudit, Long> {

    /**
     * Find all audit records for a specific notification
     */
    List<NotificationAudit> findByNotificationIdOrderByTimestampDesc(Long notificationId);

    /**
     * Find all audit records for a specific event type
     */
    List<NotificationAudit> findByEventTypeOrderByTimestampDesc(String eventType);

    /**
     * Find all audit records for a specific recipient
     */
    List<NotificationAudit> findByRecipientOrderByTimestampDesc(String recipient);

    /**
     * Find all audit records for a specific channel
     */
    List<NotificationAudit> findByChannelOrderByTimestampDesc(String channel);

    /**
     * Find all audit records between two dates
     */
    List<NotificationAudit> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime start, LocalDateTime end);

    /**
     * Find all audit records with pagination
     */
    Page<NotificationAudit> findAllByOrderByTimestampDesc(Pageable pageable);

    /**
     * Find all audit records for a specific notification with pagination
     */
    Page<NotificationAudit> findByNotificationIdOrderByTimestampDesc(
            Long notificationId, Pageable pageable);
}
