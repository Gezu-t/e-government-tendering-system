package com.egov.tendering.audit.dal.dto;

import com.egov.tendering.audit.dal.model.AuditActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning audit log entry data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private Long userId;
    private String username;
    private AuditActionType actionType;
    private String entityType;
    private String entityId;
    private String action;
    private String details;
    private String sourceIp;
    private String userAgent;
    private boolean success;
    private String failureReason;
    private LocalDateTime timestamp;
    private String correlationId;
    private String serviceId;
    private String hostName;
}
