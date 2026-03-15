package com.egov.tendering.notification.dal.repository;

import com.egov.tendering.notification.dal.model.Notification;
import com.egov.tendering.notification.dal.model.NotificationStatus;
import com.egov.tendering.notification.dal.model.NotificationType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications for a specific recipient ordered by creation date descending
     */
    @Query("SELECT n FROM Notification n JOIN n.recipients r WHERE r = :recipient ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientOrderByCreatedAtDesc(@Param("recipient") String recipient);

    @Query("SELECT DISTINCT n FROM Notification n JOIN n.recipients r WHERE r IN :recipients ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientsOrderByCreatedAtDesc(@Param("recipients") java.util.Collection<String> recipients);

    /**
     * Find all notifications for a specific recipient with pagination
     */
    @Query("SELECT n FROM Notification n JOIN n.recipients r WHERE r = :recipient ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipient(@Param("recipient") String recipient, Pageable pageable);

    /**
     * Find all unread notifications for a specific recipient
     */
    @Query("SELECT n FROM Notification n JOIN n.recipients r WHERE r = :recipient AND n.read = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByRecipient(@Param("recipient") String recipient);

    @Query("SELECT DISTINCT n FROM Notification n JOIN n.recipients r WHERE r IN :recipients AND n.read = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByRecipients(@Param("recipients") java.util.Collection<String> recipients);

    /**
     * Count unread notifications for a specific recipient
     */
    @Query("SELECT COUNT(n) FROM Notification n JOIN n.recipients r WHERE r = :recipient AND n.read = false")
    long countUnreadByRecipient(@Param("recipient") String recipient);

    @Query("SELECT COUNT(DISTINCT n) FROM Notification n JOIN n.recipients r WHERE r IN :recipients AND n.read = false")
    long countUnreadByRecipients(@Param("recipients") java.util.Collection<String> recipients);

    /**
     * Find notifications by type for a specific recipient
     */
    @Query("SELECT n FROM Notification n JOIN n.recipients r WHERE r = :recipient AND n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientAndType(@Param("recipient") String recipient, @Param("type") NotificationType type);

    /**
     * Find notifications by entity ID
     */
    List<Notification> findByEntityIdOrderByCreatedAtDesc(String entityId);

    /**
     * Find notifications created within a time range
     */
    List<Notification> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Find failed notifications that need to be retried
     */
    List<Notification> findByStatusAndLastRetryAtBefore(NotificationStatus status, LocalDateTime lastRetryBefore);

    List<Notification> findByStatusAndScheduledAtBefore(NotificationStatus status, LocalDateTime scheduledAt);

}
