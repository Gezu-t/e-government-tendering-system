package com.egov.tendering.audit.event;

import com.egov.tendering.audit.dal.model.AuditActionType;
import com.egov.tendering.audit.dal.model.AuditLog;
import com.egov.tendering.audit.dal.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    private static final List<String> ENTITY_ID_KEYS = List.of(
            "tenderId", "contractId", "bidId", "evaluationId", "reviewId", "entityId", "id"
    );
    private static final List<String> USER_ID_KEYS = List.of(
            "actorUserId", "recipientUserId", "userId", "evaluatorId", "committeeMemberId", "tendererId", "bidderId", "recipient"
    );
    private static final List<String> USERNAME_KEYS = List.of(
            "username", "createdBy", "updatedBy", "recipient", "userId"
    );

    @KafkaListener(topics = "${app.kafka.topics.tender-events:tender-events}", groupId = "${spring.application.name}")
    public void listenTenderEvents(Object event) {
        log.info("Received tender event: {}", event);
        processEvent(event, "Tender", "Tenderee");
    }

    @KafkaListener(topics = "${app.kafka.topics.evaluation-events:evaluation-events}", groupId = "${spring.application.name}")
    public void listenEvaluationEvents(Object event) {
        log.info("Received evaluation event: {}", event);
        processEvent(event, "TenderOffer", "Evaluator");
    }

    @KafkaListener(topics = "${app.kafka.topics.contract-events:contract-events}", groupId = "${spring.application.name}")
    public void listenContractEvents(Object event) {
        log.info("Received contract event: {}", event);
        processEvent(event, "Contract", "Committee");
    }

    @KafkaListener(topics = "${app.kafka.topics.bid-events:bid-events}", groupId = "${spring.application.name}")
    public void listenBidEvents(Object event) {
        log.info("Received bid event: {}", event);
        processEvent(event, "Bid", "Tenderer");
    }

    @KafkaListener(topics = "${app.kafka.topics.user-events:user-events}", groupId = "${spring.application.name}")
    public void listenUserEvents(Object event) {
        log.info("Received user event: {}", event);
        processEvent(event, "User", "System");
    }

    @KafkaListener(topics = "${app.kafka.topics.notification-events:notification-events}", groupId = "${spring.application.name}")
    public void listenNotificationEvents(Object event) {
        log.info("Received notification event: {}", event);
        processEvent(event, "Notification", "System");
    }

    private void processEvent(Object event, String entityType, String module) {
        try {
            Map<String, Object> eventData = toEventData(event);
            String eventType = extractEventType(eventData, event);
            String details = toJson(event);
            Long userId = extractUserId(eventData);
            AuditLog auditLog = AuditLog.builder()
                    .username(extractUsername(eventData, userId))
                    .actionType(extractActionType(eventType))
                    .eventType(eventType)
                    .entityType(entityType)
                    .entityId(extractEntityId(eventData))
                    .action(eventType)
                    .details(details)
                    .description(details)
                    .sourceIp("internal")
                    .success(true)
                    .timestamp(LocalDateTime.now())
                    .userId(userId)
                    .module(module)
                    .build();
            auditLogRepository.save(auditLog);
            log.info("Audit log saved: {}", auditLog);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event, e);
        }
    }

    private Map<String, Object> toEventData(Object event) {
        return objectMapper.convertValue(event, new TypeReference<>() {});
    }

    private String extractEventType(Map<String, Object> eventData, Object event) {
        Object eventType = eventData.get("eventType");
        if (eventType != null) {
            return eventType.toString();
        }

        String className = event.getClass().getSimpleName();
        return className.isBlank() ? "UNKNOWN" : className.toUpperCase();
    }

    private AuditActionType extractActionType(String eventType) {
        String normalizedEventType = eventType.toUpperCase();
        try {
            return AuditActionType.valueOf(normalizedEventType);
        } catch (IllegalArgumentException ex) {
            return AuditActionType.CUSTOM;
        }
    }

    private String extractEntityId(Map<String, Object> eventData) {
        for (String key : ENTITY_ID_KEYS) {
            Object value = eventData.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return "UNKNOWN";
    }

    private Long extractUserId(Map<String, Object> eventData) {
        for (String key : USER_ID_KEYS) {
            Object value = eventData.get(key);
            Long parsedValue = asLong(value);
            if (parsedValue != null) {
                return parsedValue;
            }
        }
        return null;
    }

    private String extractUsername(Map<String, Object> eventData, Long userId) {
        for (String key : USERNAME_KEYS) {
            Object value = eventData.get(key);
            if (value == null) {
                continue;
            }

            String stringValue = value.toString();
            if (stringValue.isBlank()) {
                continue;
            }

            if (!"recipient".equals(key) || asLong(value) == null) {
                return stringValue;
            }
        }

        if (userId != null) {
            return userId.toString();
        }

        return "system";
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize audit event payload, falling back to toString()", ex);
            return String.valueOf(event);
        }
    }
}
