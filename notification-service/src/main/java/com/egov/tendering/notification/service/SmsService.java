package com.egov.tendering.notification.service;

import com.egov.tendering.notification.dal.model.Notification;
import com.egov.tendering.notification.dal.model.NotificationStatus;
import com.egov.tendering.notification.dal.repository.NotificationRepository;
import com.egov.tendering.notification.event.NotificationEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final TemplateService templateService;
    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher eventPublisher;

    // This could be replaced with an actual SMS gateway client
    // such as Twilio, AWS SNS, or other SMS service providers
    private final SmsSender smsSender;

    private static final int MAX_RETRY_ATTEMPTS = 3;

    public void sendSms(Notification notification) {
        try {
            log.info("Sending SMS notification: {}", notification.getId());

            if (notification.getRetryCount() == null) {
                notification.setRetryCount(0);
            }

            for (String recipient : notification.getRecipients()) {
                sendSmsToRecipient(notification, recipient);
            }

            // Update notification status
            notification.setStatus(NotificationStatus.DELIVERED);
            notification.setDeliveredAt(LocalDateTime.now());
            notificationRepository.save(notification);

            // Publish event
            eventPublisher.publishSmsSentEvent(notification);
        } catch (Exception e) {
            handleSendingError(notification, e);
        }
    }

    private void sendSmsToRecipient(Notification notification, String recipient) {
        // Process the template if needed
        String smsContent = templateService.processTemplate(
                notification.getType().name().toLowerCase() + "_sms",
                notification
        );

        // Send the SMS
        smsSender.send(recipient, smsContent);
        log.info("SMS sent to: {}", recipient);
    }

    private void handleSendingError(Notification notification, Exception exception) {
        log.error("Failed to send SMS notification: {}", notification.getId(), exception);

        // Update notification with error
        notification.setStatus(NotificationStatus.FAILED);
        notification.setErrorMessage(exception.getMessage());

        // Check if we should retry
        if (notification.getRetryCount() < MAX_RETRY_ATTEMPTS) {
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setLastRetryAt(LocalDateTime.now());
            notification.setStatus(NotificationStatus.PENDING_RETRY);
            log.info("Scheduled for retry, attempt {} of {}",
                    notification.getRetryCount(), MAX_RETRY_ATTEMPTS);

            // Publish retry event
            eventPublisher.publishNotificationRetryEvent(notification);
        } else {
            log.warn("Max retry attempts reached for notification: {}", notification.getId());
            // Publish failure event
            eventPublisher.publishNotificationFailedEvent(notification, exception.getMessage());
        }

        notificationRepository.save(notification);
    }

    // Interface for SMS sending implementation
    public interface SmsSender {
        void send(String phoneNumber, String message);
    }
}
