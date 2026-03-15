package com.egov.tendering.document.event;

import com.egov.tendering.document.dal.model.DocumentStatus;
import com.egov.tendering.document.dal.model.DocumentType;
import com.egov.tendering.document.dal.model.EntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Base event for document events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEvent {
    private Long documentId;
    private String name;
    private DocumentType documentType;
    private String entityId;
    private EntityType entityType;
    private DocumentStatus status;
    private String userId;
    private Long actorUserId;
    private LocalDateTime timestamp;
    private DocumentEventType eventType;
}
