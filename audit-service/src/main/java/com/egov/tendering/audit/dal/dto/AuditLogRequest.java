package com.egov.tendering.audit.dal.dto;

import com.egov.tendering.audit.dal.model.AuditActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new audit log entry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequest {

    // The user who performed the action
    private Long userId;
    private String username;

    @NotNull(message = "Action type is required")
    private AuditActionType actionType;

    @NotBlank(message = "Entity type is required")
    private String entityType;

    private String entityId;

    @NotBlank(message = "Action is required")
    private String action;

    private String details;
    private String sourceIp;
    private String userAgent;
    private boolean success = true;
    private String failureReason;
    private String correlationId;
    private String serviceId;
}
