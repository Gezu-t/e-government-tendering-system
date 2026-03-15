package com.egov.tendering.audit.dal.dto;

import com.egov.tendering.audit.dal.model.AuditActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDTO {

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

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime timestamp;

    private String correlationId;
    private String serviceId;
    private String hostName;
}
