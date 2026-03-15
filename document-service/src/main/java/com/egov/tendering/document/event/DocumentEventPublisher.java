package com.egov.tendering.document.event;

import com.egov.tendering.document.dal.model.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Publisher for document events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentEventPublisher {

    private final KafkaTemplate<String, DocumentEvent> kafkaTemplate;

    @Value("${kafka.topics.document-events}")
    private String documentEventsTopic;

    /**
     * Publish document uploaded event
     */
    public void publishDocumentUploadedEvent(Document document) {
        DocumentEvent event = buildDocumentEvent(document, DocumentEventType.UPLOADED, document.getCreatedBy());
        publishEvent(event, "document-uploaded");
    }

    /**
     * Publish document updated event
     */
    public void publishDocumentUpdatedEvent(Document document, String userId) {
        DocumentEvent event = buildDocumentEvent(document, DocumentEventType.UPDATED, userId);
        publishEvent(event, "document-updated");
    }

    /**
     * Publish document downloaded event
     */
    public void publishDocumentDownloadedEvent(Document document, String userId) {
        DocumentEvent event = buildDocumentEvent(document, DocumentEventType.DOWNLOADED, userId);
        publishEvent(event, "document-downloaded");
    }

    /**
     * Publish document deleted event
     */
    public void publishDocumentDeletedEvent(Document document, String userId) {
        DocumentEvent event = buildDocumentEvent(document, DocumentEventType.DELETED, userId);
        publishEvent(event, "document-deleted");
    }

    /**
     * Publish document expired event
     */
    public void publishDocumentExpiredEvent(Document document) {
        DocumentEvent event = buildDocumentEvent(document, DocumentEventType.EXPIRED, "system");
        publishEvent(event, "document-expired");
    }

    /**
     * Publish document verified event
     */
    public void publishDocumentVerifiedEvent(Document document, String userId) {
        DocumentEvent event = buildDocumentEvent(document, DocumentEventType.VERIFIED, userId);
        publishEvent(event, "document-verified");
    }

    /**
     * Publish document rejected event
     */
    public void publishDocumentRejectedEvent(Document document, String userId) {
        DocumentEvent event = buildDocumentEvent(document, DocumentEventType.REJECTED, userId);
        publishEvent(event, "document-rejected");
    }

    /**
     * Build document event from document entity
     */
    private DocumentEvent buildDocumentEvent(Document document, DocumentEventType eventType, String userId) {
        return DocumentEvent.builder()
                .documentId(document.getId())
                .name(document.getName())
                .documentType(document.getDocumentType())
                .entityId(document.getEntityId())
                .entityType(document.getEntityType())
                .status(document.getStatus())
                .userId(userId)
                .actorUserId(parseUserId(userId))
                .timestamp(LocalDateTime.now())
                .eventType(eventType)
                .build();
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Publish event to Kafka
     */
    private void publishEvent(DocumentEvent event, String eventDescription) {
        log.info("Publishing {} event for document: {}", eventDescription, event.getDocumentId());

        try {
            CompletableFuture<SendResult<String, DocumentEvent>> future = kafkaTemplate.send(
                    documentEventsTopic,
                    event.getDocumentId().toString(),
                    event
            );

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} event for document: {}", eventDescription, event.getDocumentId(), ex);
                } else {
                    log.debug("Successfully published {} event for document: {}", eventDescription, event.getDocumentId());
                }
            });
        } catch (Exception e) {
            log.error("Exception publishing {} event for document: {}", eventDescription, event.getDocumentId(), e);
        }
    }
}
