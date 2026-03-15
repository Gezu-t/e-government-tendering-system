package com.egov.tendering.audit.dal.dto;

import com.egov.tendering.audit.dal.model.AuditActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for filtering audit logs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogFilter {

    private Long userId;
    private String username;
    private List<AuditActionType> actionTypes;
    private String entityType;
    private String entityId;
    private String action;
    private Boolean success;
    private String keyword;
    private String correlationId;
    private String serviceId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}
