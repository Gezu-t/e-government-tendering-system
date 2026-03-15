package com.egov.tendering.tender.event;

import com.egov.tendering.tender.dal.model.Tender;
import com.egov.tendering.tender.dal.model.TenderAmendment;
import com.egov.tendering.tender.dal.model.TenderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenderEventPublisher {

    private final KafkaTemplate<String, TenderEvent> kafkaTemplate;
    private final AtomicLong eventIdCounter = new AtomicLong(1); // For sequential IDs

    @Value("${app.kafka.topics.tender-events}")
    private String tenderEventsTopic;

    public void publishTenderCreatedEvent(Tender tender) {
        TenderCreatedEvent event = TenderCreatedEvent.builder()
                .eventId(generateEventId())
                .eventType("TENDER_CREATED")
                .timestamp(LocalDateTime.now())
                .tenderId(tender.getId())
                .tenderTitle(tender.getTitle())
                .actorUserId(tender.getTendereeId())
                .tendereeId(tender.getTendereeId())
                .type(tender.getType())
                .submissionDeadline(tender.getSubmissionDeadline())
                .allocationStrategy(tender.getAllocationStrategy())
                .build();

        log.info("Publishing tender created event: {}", event);
        kafkaTemplate.send(tenderEventsTopic, tender.getId().toString(), event);
    }

    public void publishTenderStatusChangedEvent(Tender tender, TenderStatus oldStatus) {
        TenderStatusChangedEvent event = TenderStatusChangedEvent.builder()
                .eventId(generateEventId())
                .eventType("TENDER_STATUS_CHANGED")
                .timestamp(LocalDateTime.now())
                .tenderId(tender.getId())
                .tenderTitle(tender.getTitle())
                .actorUserId(tender.getTendereeId())
                .oldStatus(oldStatus)
                .newStatus(tender.getStatus())
                .build();

        log.info("Publishing tender status changed event: {}", event);
        kafkaTemplate.send(tenderEventsTopic, tender.getId().toString(), event);
    }

    public void publishTenderPublishedEvent(Tender tender) {
        TenderPublishedEvent event = TenderPublishedEvent.builder()
                .eventId(generateEventId())
                .eventType("TENDER_PUBLISHED")
                .timestamp(LocalDateTime.now())
                .tenderId(tender.getId())
                .tenderTitle(tender.getTitle())
                .actorUserId(tender.getTendereeId())
                .submissionDeadline(tender.getSubmissionDeadline())
                .build();

        log.info("Publishing tender published event: {}", event);
        kafkaTemplate.send(tenderEventsTopic, tender.getId().toString(), event);
    }

    public void publishTenderClosedEvent(Tender tender) {
        boolean expired = LocalDateTime.now().isAfter(tender.getSubmissionDeadline());

        TenderClosedEvent event = TenderClosedEvent.builder()
                .eventId(generateEventId())
                .eventType("TENDER_CLOSED")
                .timestamp(LocalDateTime.now())
                .tenderId(tender.getId())
                .tenderTitle(tender.getTitle())
                .actorUserId(tender.getTendereeId())
                .expired(expired)
                .build();

        log.info("Publishing tender closed event: {}", event);
        kafkaTemplate.send(tenderEventsTopic, tender.getId().toString(), event);
    }

    public void publishTenderAmendedEvent(Tender tender, TenderAmendment amendment) {
        TenderAmendedEvent event = TenderAmendedEvent.builder()
                .eventId(generateEventId())
                .eventType("TENDER_AMENDED")
                .timestamp(LocalDateTime.now())
                .tenderId(tender.getId())
                .tenderTitle(tender.getTitle())
                .actorUserId(amendment.getAmendedBy())
                .amendmentNumber(amendment.getAmendmentNumber())
                .reason(amendment.getReason())
                .previousDeadline(amendment.getPreviousDeadline())
                .newDeadline(amendment.getNewDeadline())
                .amendedBy(amendment.getAmendedBy())
                .build();

        log.info("Publishing tender amended event: {}", event);
        kafkaTemplate.send(tenderEventsTopic, tender.getId().toString(), event);
    }

    /**
     * Generate a unique event ID.
     * Choose ONE of the following approaches based on your requirements:
     */
    private Long generateEventId() {
        // Option 1: Sequential counter (thread-safe)
        return eventIdCounter.getAndIncrement();

        // Option 2: Current timestamp
        // return System.currentTimeMillis();

        // Option 3: Random long value
        // return new Random().nextLong();

        // Option 4: UUID-based (if your event uses Long for ID)
        // return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }
}
