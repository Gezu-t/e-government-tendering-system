package com.egov.tendering.notification.event;

import com.egov.tendering.notification.dal.model.Notification;
import com.egov.tendering.notification.dal.model.NotificationStatus;
import com.egov.tendering.notification.dal.repository.NotificationRepository;
import com.egov.tendering.notification.service.EmailService;
import com.egov.tendering.notification.service.NotificationAuditService;
import com.egov.tendering.notification.service.PushNotificationService;
import com.egov.tendering.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushNotificationService;
    private final NotificationAuditService auditService;

    @Value("${spring.application.name:notification-service}")
    private String applicationName;

    @KafkaListener(
            topics = "${kafka.topics.notification-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-events-consumer}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Received notification event: {}", event);

        try {
            // Audit all events
            auditService.auditEvent(event);

            if (event instanceof NotificationRetryEvent) {
                handleRetryEvent((NotificationRetryEvent) event);
            } else if (event instanceof NotificationFailedEvent) {
                handleFailedEvent((NotificationFailedEvent) event);
            } else if (event instanceof EmailSentEvent) {
                handleEmailSentEvent((EmailSentEvent) event);
            } else if (event instanceof SmsSentEvent) {
                handleSmsSentEvent((SmsSentEvent) event);
            } else if (event instanceof PushSentEvent) {
                handlePushSentEvent((PushSentEvent) event);
            } else {
                log.warn("Received unsupported event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing notification event: {}", event, e);
        }
    }

    private void handleRetryEvent(NotificationRetryEvent event) {
        log.info("Processing retry event for notification: {}", event.getNotificationId());

        Optional<Notification> notificationOpt = notificationRepository.findById(event.getNotificationId());
        if (notificationOpt.isEmpty()) {
            log.warn("Cannot retry notification: {} - not found", event.getNotificationId());
            return;
        }

        Notification notification = notificationOpt.get();

        // Only process if the notification is in PENDING_RETRY status
        if (notification.getStatus() != NotificationStatus.PENDING_RETRY) {
            log.info("Skipping retry for notification: {} - current status: {}",
                    notification.getId(), notification.getStatus());
            return;
        }

        // Update metrics or logs about retry attempts
        log.info("Notification {} retry attempt {} initiated",
                notification.getId(), notification.getRetryCount());
    }

    private void handleFailedEvent(NotificationFailedEvent event) {
        log.info("Processing failed event for notification: {}", event.getNotificationId());

        Optional<Notification> notificationOpt = notificationRepository.findById(event.getNotificationId());
        if (notificationOpt.isEmpty()) {
            log.warn("Failed notification not found: {}", event.getNotificationId());
            return;
        }

        Notification notification = notificationOpt.get();

        // Log or process the permanent failure
        log.warn("Notification {} permanently failed after {} attempts. Error: {}",
                notification.getId(),
                notification.getRetryCount(),
                event.getErrorMessage());

        // You could implement escalation logic here, such as:
        // 1. Send an alert to admin
        // 2. Create a support ticket
        // 3. Log to a separate failure database
    }

    private void handleEmailSentEvent(EmailSentEvent event) {
        log.info("Processing email sent event for notification: {}", event.getNotificationId());

        // This could be used for analytics, auditing, or confirmation processing
        updateNotificationStatus(event.getNotificationId(), NotificationStatus.DELIVERED);
    }

    private void handleSmsSentEvent(SmsSentEvent event) {
        log.info("Processing SMS sent event for notification: {}", event.getNotificationId());

        // This could be used for analytics, auditing, or confirmation processing
        updateNotificationStatus(event.getNotificationId(), NotificationStatus.DELIVERED);
    }

    private void handlePushSentEvent(PushSentEvent event) {
        log.info("Processing push sent event for notification: {}", event.getNotificationId());

        // This could be used for analytics, auditing, or confirmation processing
        updateNotificationStatus(event.getNotificationId(), NotificationStatus.DELIVERED);
    }

    private void updateNotificationStatus(Long notificationId, NotificationStatus status) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            // Only update if the status is changing
            if (notification.getStatus() != status) {
                notification.setStatus(status);

                if (status == NotificationStatus.DELIVERED && notification.getDeliveredAt() == null) {
                    notification.setDeliveredAt(LocalDateTime.now());
                }

                notificationRepository.save(notification);
                log.debug("Updated notification {} status to {}", notificationId, status);
            }
        });
    }
}
