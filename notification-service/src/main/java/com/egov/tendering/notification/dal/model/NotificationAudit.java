package com.egov.tendering.notification.dal.model;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to store notification audit records for tracking all notification events
 */
@Entity
@Table(name = "notification_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long notificationId;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String eventType;

    @Column
    private String channel;

    @Column
    private String recipient;

    @Column
    private String subject;

    @Column
    private String status;

    @Column(length = 1000)
    private String errorMessage;

    @Column
    private Integer retryCount;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private LocalDateTime auditTimestamp;
}
