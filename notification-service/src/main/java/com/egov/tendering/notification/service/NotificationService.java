package com.egov.tendering.notification.service;

import com.egov.tendering.notification.config.KafkaTopics;
import com.egov.tendering.notification.dal.dto.NotificationRequest;
import com.egov.tendering.notification.dal.dto.NotificationResponse;
import com.egov.tendering.notification.dal.model.Notification;
import com.egov.tendering.notification.dal.model.NotificationStatus;
import com.egov.tendering.notification.dal.model.NotificationType;
import com.egov.tendering.notification.dal.repository.NotificationRepository;
import com.egov.tendering.notification.event.GenericNotificationEvent;
import com.egov.tendering.notification.event.NotificationEvent;
import com.egov.tendering.notification.event.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushNotificationService;
    private final KafkaTemplate<String, GenericNotificationEvent> kafkaTemplate;
    private final NotificationEventPublisher eventPublisher;

    @KafkaListener(topics = "${kafka.topics.tender-created}")
    public void handleTenderCreated(GenericNotificationEvent event) {
        log.info("Received tender created event: {}", event);
        sendNotification(
                NotificationType.TENDER_CREATED,
                String.valueOf(event.getEntityId()),
                "New Tender Created",
                "A new tender has been created: " + event.getEntityId(),
                event.getRecipients()
        );
    }

    @KafkaListener(topics = "${kafka.topics.tender-updated}")
    public void handleTenderUpdated(GenericNotificationEvent event) {
        log.info("Received tender updated event: {}", event);
        sendNotification(
                NotificationType.TENDER_UPDATED,
                event.getEntityId(),
                "Tender Updated",
                "Tender has been updated: " + event.getEntityId(),
                event.getRecipients()
        );
    }

    @KafkaListener(topics = "${kafka.topics.bid-submitted}")
    public void handleBidSubmitted(GenericNotificationEvent event) {
        log.info("Received bid submitted event: {}", event);

        sendNotification(
                NotificationType.BID_SUBMITTED,
                event.getEntityId(),
                "New Bid Submitted",
                "A new bid has been submitted for tender: " + event.getEntityId(),
                safeRecipients(event)
        );
    }

    @KafkaListener(topics = "${kafka.topics.bid-evaluation-completed}")
    public void handleBidEvaluationCompleted(GenericNotificationEvent event) {
        log.info("Received bid evaluation completed event: {}", event);

        sendNotification(
                NotificationType.BID_EVALUATION_COMPLETED,
                event.getEntityId(),
                "Bid Evaluation Completed",
                "Bid evaluation has been completed for tender: " + event.getEntityId(),
                safeRecipients(event)
        );
    }

    @KafkaListener(topics = "${kafka.topics.contract-awarded}")
    public void handleContractAwarded(GenericNotificationEvent event) {
        log.info("Received contract awarded event: {}", event);

        sendNotification(
                NotificationType.CONTRACT_AWARDED,
                event.getEntityId(),
                "Contract Awarded",
                "A contract has been awarded for tender: " + event.getEntityId(),
                safeRecipients(event)
        );
    }

    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("Sending notification: {}", request);
        Notification notification = createNotification(
                request.getType(),
                request.getEntityId(),
                request.getSubject(),
                request.getMessage(),
                request.getRecipients()
        );

        deliverNotification(notification);

        GenericNotificationEvent event = createGenericNotificationEvent(notification);
        kafkaTemplate.send(KafkaTopics.NOTIFICATION_SENT, event);

        return new NotificationResponse(
                notification.getId(),
                notification.getStatus(),
                "Notification sent successfully"
        );
    }

    @Transactional
    public NotificationResponse scheduleNotification(NotificationRequest request, LocalDateTime scheduledTime) {
        log.info("Scheduling notification for {}: {}", scheduledTime, request);

        Notification notification = createNotification(
                request.getType(),
                request.getEntityId(),
                request.getSubject(),
                request.getMessage(),
                request.getRecipients()
        );

        // Set scheduled time
        notification.setStatus(NotificationStatus.SCHEDULED);
        notification.setScheduledAt(scheduledTime);

        // Save to repository
        notification = notificationRepository.save(notification);

        log.info("Notification scheduled with ID: {}", notification.getId());

        return new NotificationResponse(
                notification.getId(),
                notification.getStatus(),
                "Notification scheduled successfully"
        );
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(String userId) {
        log.info("Fetching notifications for user: {}", userId);
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUsers(Collection<String> userIdentifiers) {
        Set<String> identifiers = normalizeIdentifiers(userIdentifiers);
        log.info("Fetching notifications for identifiers: {}", identifiers);
        if (identifiers.isEmpty()) {
            return List.of();
        }
        return notificationRepository.findByRecipientsOrderByCreatedAtDesc(identifiers);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsForUser(String userId) {
        log.info("Fetching unread notifications for user: {}", userId);
        return notificationRepository.findUnreadByRecipient(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsForUsers(Collection<String> userIdentifiers) {
        Set<String> identifiers = normalizeIdentifiers(userIdentifiers);
        log.info("Fetching unread notifications for identifiers: {}", identifiers);
        if (identifiers.isEmpty()) {
            return List.of();
        }
        return notificationRepository.findUnreadByRecipients(identifiers);
    }

    @Transactional(readOnly = true)
    public long countUnreadNotifications(String userId) {
        log.info("Counting unread notifications for user: {}", userId);
        return notificationRepository.countUnreadByRecipient(userId);
    }

    @Transactional(readOnly = true)
    public long countUnreadNotifications(Collection<String> userIdentifiers) {
        Set<String> identifiers = normalizeIdentifiers(userIdentifiers);
        log.info("Counting unread notifications for identifiers: {}", identifiers);
        if (identifiers.isEmpty()) {
            return 0L;
        }
        return notificationRepository.countUnreadByRecipients(identifiers);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Notification> getNotification(Long notificationId) {
        log.info("Fetching notification details: {}", notificationId);
        return notificationRepository.findById(notificationId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        log.info("Marking notification as read: {}", notificationId);
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    private void sendNotification(NotificationType type, String entityId, String subject, String message, List<String> recipients) {
        Notification notification = createNotification(type, entityId, subject, message, recipients);
        deliverNotification(notification);
    }

    private Notification createNotification(NotificationType type, String entityId, String subject, String message, List<String> recipients) {
        List<String> normalizedRecipients = normalizeRecipientList(recipients);
        if (normalizedRecipients.isEmpty()) {
            throw new IllegalArgumentException("Notification recipients cannot be empty");
        }

        Notification notification = new Notification();
        notification.setType(type);
        notification.setEntityId(entityId);
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setRecipients(normalizedRecipients);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);

        return notificationRepository.save(notification);
    }

    private Set<String> normalizeIdentifiers(Collection<String> userIdentifiers) {
        Set<String> identifiers = new LinkedHashSet<>();
        if (userIdentifiers == null) {
            return identifiers;
        }
        for (String identifier : userIdentifiers) {
            if (identifier != null && !identifier.isBlank()) {
                identifiers.add(identifier);
            }
        }
        return identifiers;
    }

    private List<String> normalizeRecipientList(Collection<String> recipients) {
        return normalizeIdentifiers(recipients).stream().toList();
    }

    private void deliverNotification(Notification notification) {
        try {
            notification.setSentAt(LocalDateTime.now());

            switch (notification.getType().getChannel()) {
                case EMAIL:
                    emailService.sendEmail(notification);
                    break;
                case SMS:
                    smsService.sendSms(notification);
                    break;
                case PUSH:
                    pushNotificationService.sendPushNotification(notification);
                    break;
                case DASHBOARD:
                    // Just save to repository - no delivery needed
                    notification.setStatus(NotificationStatus.DELIVERED);
                    notification.setDeliveredAt(LocalDateTime.now());
                    break;
                default:
                    log.warn("Unknown channel for notification type: {}", notification.getType());
                    notification.setStatus(NotificationStatus.FAILED);
                    notification.setErrorMessage("Unknown notification channel");
                    break;
            }

            // If notification was delivered successfully
            if (notification.getStatus() == NotificationStatus.DELIVERED) {
                // Publish appropriate event based on channel
                if (notification.getType().getChannel() == com.egov.tendering.notification.dal.model.NotificationChannel.EMAIL) {
                    eventPublisher.publishEmailSentEvent(notification);
                } else if (notification.getType().getChannel() == com.egov.tendering.notification.dal.model.NotificationChannel.SMS) {
                    eventPublisher.publishSmsSentEvent(notification);
                } else if (notification.getType().getChannel() == com.egov.tendering.notification.dal.model.NotificationChannel.PUSH) {
                    eventPublisher.publishPushSentEvent(notification);
                }
            }

        } catch (Exception e) {
            log.error("Failed to deliver notification: {}", notification.getId(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());

            // Increment retry count if this should be retried
            if (notification.getRetryCount() < 3) { // Max 3 retries
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setStatus(NotificationStatus.PENDING_RETRY);
                notification.setLastRetryAt(LocalDateTime.now());

                // Publish retry event
                eventPublisher.publishNotificationRetryEvent(notification);
            } else {
                // Publish failure event
                eventPublisher.publishNotificationFailedEvent(notification, e.getMessage());
            }
        }

        notificationRepository.save(notification);
    }

    /**
     * Create a generic notification event for Kafka
     * This is different from the specific event types used by NotificationEventPublisher
     *
     * @param notification The notification entity
     * @return A generic notification event
     */
    private GenericNotificationEvent createGenericNotificationEvent(Notification notification) {
        return GenericNotificationEvent.builder()
                .id(String.valueOf(notification.getId()))
                .type(notification.getType())
                .entityId(notification.getEntityId())
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .recipients(notification.getRecipients())
                .status(notification.getStatus().name())
                .createdAt(notification.getCreatedAt())
                .deliveredAt(notification.getDeliveredAt())
                .sentAt(notification.getSentAt())
                .build();
    }

    private List<String> safeRecipients(GenericNotificationEvent event) {
        return event.getRecipients() != null ? event.getRecipients() : List.of();
    }
}
