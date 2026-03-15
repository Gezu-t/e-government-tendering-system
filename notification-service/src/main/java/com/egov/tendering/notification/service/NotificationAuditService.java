package com.egov.tendering.notification.service;

import com.egov.tendering.notification.dal.model.NotificationAudit;
import com.egov.tendering.notification.dal.model.NotificationStatus;
import com.egov.tendering.notification.dal.repository.NotificationAuditRepository;
import com.egov.tendering.notification.event.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
/**
 * Service for auditing all notification-related events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAuditService {

    private final NotificationAuditRepository auditRepository;

    /**
     * Record an audit entry for a notification event
     */
    @Transactional
    public NotificationAudit auditEvent(NotificationEvent event) {
        log.debug("Auditing notification event: {}", event.getEventType());

        NotificationAudit audit = createBaseAuditRecord(event);

        // Populate event-specific details
        if (event instanceof EmailSentEvent) {
            populateEmailSentDetails(audit, (EmailSentEvent) event);
        } else if (event instanceof SmsSentEvent) {
            populateSmsSentDetails(audit, (SmsSentEvent) event);
        } else if (event instanceof PushSentEvent) {
            populatePushSentDetails(audit, (PushSentEvent) event);
        } else if (event instanceof NotificationFailedEvent) {
            populateFailedDetails(audit, (NotificationFailedEvent) event);
        } else if (event instanceof NotificationRetryEvent) {
            populateRetryDetails(audit, (NotificationRetryEvent) event);
        }

        return auditRepository.save(audit);
    }

    private NotificationAudit createBaseAuditRecord(NotificationEvent event) {
        NotificationAudit audit = new NotificationAudit();
        audit.setNotificationId(event.getNotificationId());
        audit.setEventId(event.getEventId());
        audit.setEventType(event.getEventType().name());
        audit.setTimestamp(event.getTimestamp());
        audit.setAuditTimestamp(LocalDateTime.now());
        return audit;
    }

    private void populateEmailSentDetails(NotificationAudit audit, EmailSentEvent event) {
        audit.setChannel("EMAIL");
        audit.setRecipient(String.valueOf(event.getRecipient()));
        audit.setSubject(event.getSubject());
        audit.setStatus(event.getStatus());
    }

    private void populateSmsSentDetails(NotificationAudit audit, SmsSentEvent event) {
        audit.setChannel("SMS");
        audit.setRecipient(event.getRecipient());
        audit.setStatus(event.getStatus());
    }

    private void populatePushSentDetails(NotificationAudit audit, PushSentEvent event) {
        audit.setChannel("PUSH");
        audit.setRecipient(event.getRecipient());
        audit.setSubject(event.getTitle());
        audit.setStatus(event.getStatus());
    }

    private void populateFailedDetails(NotificationAudit audit, NotificationFailedEvent event) {
        audit.setChannel(event.getChannel());
        audit.setRecipient(event.getRecipient());
        audit.setErrorMessage(event.getErrorMessage());
        audit.setRetryCount(event.getRetryCount());
        audit.setStatus(NotificationStatus.FAILED.name());
    }

    private void populateRetryDetails(NotificationAudit audit, NotificationRetryEvent event) {
        audit.setChannel(event.getChannel());
        audit.setRecipient(event.getRecipient());
        audit.setRetryCount(event.getRetryCount());
        audit.setStatus(NotificationStatus.PENDING_RETRY.name());
    }
}
