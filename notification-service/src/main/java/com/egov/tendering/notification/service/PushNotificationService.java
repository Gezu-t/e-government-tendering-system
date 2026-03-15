package com.egov.tendering.notification.service;

import com.egov.tendering.notification.dal.model.Notification;
import com.egov.tendering.notification.exception.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final TemplateService templateService;

    // This could be replaced with an actual push notification service
    // such as Firebase Cloud Messaging, OneSignal, or other providers
    private final PushSender pushSender;

    public void sendPushNotification(Notification notification) {
        log.info("Sending push notification: {}", notification.getId());

        for (String recipient : notification.getRecipients()) {
            try {
                // Process the template if needed
                String notificationContent = templateService.processTemplate(
                        notification.getType().name().toLowerCase() + "_push",
                        notification
                );

                // Send the push notification
                pushSender.send(
                        recipient,
                        notification.getSubject(),
                        notificationContent,
                        notification.getEntityId()
                );

                log.info("Push notification sent to: {}", recipient);
            } catch (Exception e) {
                log.error("Failed to send push notification to recipient: {}", recipient, e);
                throw new MessagingException("Failed to send push notification", e);
            }
        }
    }

    // Interface for push notification implementation
    public interface PushSender {
        void send(String userId, String title, String body, String data);
    }
}
