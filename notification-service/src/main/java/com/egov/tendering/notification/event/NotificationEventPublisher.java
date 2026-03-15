package com.egov.tendering.notification.event;

import com.egov.tendering.notification.dal.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static com.egov.tendering.notification.event.NotificationEventType.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final AtomicLong eventIdCounter = new AtomicLong(1);

    @Value("${app.kafka.topics.notification-events}")
    private String notificationEventsTopic;

    public void publishEmailSentEvent(Notification notification) {
        if (notification == null || notification.getId() == null) {
            log.error("Cannot publish event for null notification or notification without ID");
            return;
        }

        // For compatibility with our multi-recipient implementation, we'll send an event for each recipient
        for (String recipient : notification.getRecipients()) {
            EmailSentEvent event = EmailSentEvent.builder()
                    .eventId(generateEventId())
                    .eventType(EMAIL_SENT)
                    .timestamp(LocalDateTime.now())
                    .notificationId(notification.getId())
                    .recipient(recipient)
                    .subject(notification.getSubject())
                    .content(notification.getMessage())
                    .status(notification.getStatus().name())
                    .build();

            sendEventWithErrorHandling(
                    notification.getId() + "_" + recipient,
                    event,
                    "email sent"
            );
        }
    }

    public void publishSmsSentEvent(Notification notification) {
        if (notification == null || notification.getId() == null) {
            log.error("Cannot publish event for null notification or notification without ID");
            return;
        }

        for (String recipient : notification.getRecipients()) {
            SmsSentEvent event = SmsSentEvent.builder()
                    .eventId(generateEventId())
                    .eventType(SMS_SENT)
                    .timestamp(LocalDateTime.now())
                    .notificationId(notification.getId())
                    .recipient(recipient)
                    .content(notification.getMessage())
                    .status(notification.getStatus().name())
                    .build();

            sendEventWithErrorHandling(
                    notification.getId() + "_" + recipient,
                    event,
                    "sms sent"
            );
        }
    }

    public void publishPushSentEvent(Notification notification) {
        if (notification == null || notification.getId() == null) {
            log.error("Cannot publish event for null notification or notification without ID");
            return;
        }

        for (String recipient : notification.getRecipients()) {
            PushSentEvent event = PushSentEvent.builder()
                    .eventId(generateEventId())
                    .eventType(PUSH_SENT)
                    .timestamp(LocalDateTime.now())
                    .notificationId(notification.getId())
                    .recipient(recipient)
                    .title(notification.getSubject())
                    .content(notification.getMessage())
                    .status(notification.getStatus().name())
                    .build();

            sendEventWithErrorHandling(
                    notification.getId() + "_" + recipient,
                    event,
                    "push sent"
            );
        }
    }

    public void publishNotificationFailedEvent(Notification notification, String errorMessage) {
        if (notification == null || notification.getId() == null) {
            log.error("Cannot publish failed event for null notification");
            return;
        }

        NotificationFailedEvent event = NotificationFailedEvent.builder()
                .eventId(generateEventId())
                .eventType(NOTIFICATION_FAILED)
                .timestamp(LocalDateTime.now())
                .notificationId(notification.getId())
                .channel(mapChannel(notification.getType().getChannel()))
                .recipient(String.join(",", notification.getRecipients()))
                .errorMessage(errorMessage)
                .retryCount(notification.getRetryCount() != null ? notification.getRetryCount() : 0)
                .build();

        sendEventWithErrorHandling(
                notification.getId() + "_failed",
                event,
                "notification failed"
        );
    }

    public void publishNotificationRetryEvent(Notification notification) {
        if (notification == null || notification.getId() == null) {
            log.error("Cannot publish retry event for null notification");
            return;
        }

        NotificationRetryEvent event = NotificationRetryEvent.builder()
                .eventId(generateEventId())
                .eventType(NOTIFICATION_RETRY)
                .timestamp(LocalDateTime.now())
                .notificationId(notification.getId())
                .channel(mapChannel(notification.getType().getChannel()))
                .recipient(String.join(",", notification.getRecipients()))
                .retryCount(notification.getRetryCount() != null ? notification.getRetryCount() : 0)
                .build();

        sendEventWithErrorHandling(
                notification.getId() + "_retry_" + notification.getRetryCount(),
                event,
                "notification retry"
        );
    }

    private <T extends NotificationEvent> void sendEventWithErrorHandling(
            String key,
            T event,
            String eventDescription
    ) {
        log.info("Publishing {} event: {}", eventDescription, event);

        try {
            CompletableFuture<SendResult<String, NotificationEvent>> future = kafkaTemplate.send(
                    notificationEventsTopic,
                    key,
                    event
            );

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} event with key: {}", eventDescription, key, ex);
                } else {
                    log.debug("Successfully published {} event to partition {} offset {}",
                            eventDescription,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Exception publishing {} event: {}", eventDescription, e.getMessage());
        }
    }

    private Long generateEventId() {
        return eventIdCounter.getAndIncrement();
    }

    // Map NotificationChannel enum to String
    private String mapChannel(com.egov.tendering.notification.dal.model.NotificationChannel channel) {
        if (channel == null) {
            return "UNKNOWN";
        }

        return channel.name();
    }
}
