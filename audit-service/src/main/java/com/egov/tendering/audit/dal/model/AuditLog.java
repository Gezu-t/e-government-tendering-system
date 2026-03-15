package com.egov.tendering.audit.dal.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entity representing an audit log entry
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private AuditActionType actionType;

    @Column(nullable = false)
    private String entityType;

    @Column
    private String entityId;

    @Column(nullable = false)
    private String action;

    @Column(length = 4000)
    private String details;

    @Column(nullable = false)
    private String sourceIp;

    @Column
    private String userAgent;

    @Column(nullable = false)
    private boolean success;

    @Column
    private String failureReason;

    private String eventType;

    private String description;

    private String module;

    private String subModule;


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column
    private String correlationId;

    @Column
    private String serviceId;

    @Column
    private String hostName;
}
