package com.egov.tendering.notification.service;


import com.egov.tendering.notification.dal.model.Notification;
import com.egov.tendering.notification.dal.model.NotificationChannel;
import com.egov.tendering.notification.dal.model.NotificationStatus;
import com.egov.tendering.notification.dal.repository.NotificationRepository;
import com.egov.tendering.notification.exception.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for handling scheduled notifications and retries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSchedulerService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushNotificationService;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MINUTES = 5;

    /**
     * Process scheduled notifications that are due to be sent
     * Runs every minute by default
     */
    @Scheduled(fixedRateString = "${notification.scheduler.interval:60000}")
    @Transactional
    public void processScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Processing scheduled notifications at {}", now);

        List<Notification> dueNotifications = notificationRepository.findByStatusAndScheduledAtBefore(
                NotificationStatus.SCHEDULED, now);

        if (dueNotifications.isEmpty()) {
            log.debug("No scheduled notifications due for processing");
            return;
        }

        log.info("Found {} scheduled notifications to process", dueNotifications.size());

        for (Notification notification : dueNotifications) {
            processScheduledNotification(notification);
        }
    }

    /**
     * Retry failed notifications
     * Runs every minute by default
     */
    @Scheduled(fixedRateString = "${notification.retry.interval:60000}")
    @Transactional
    public void retryFailedNotifications() {
        log.debug("Checking for notifications to retry");

        // Find notifications that are due for retry (waited at least RETRY_DELAY_MINUTES)
        LocalDateTime retryBefore = LocalDateTime.now().minus(RETRY_DELAY_MINUTES, ChronoUnit.MINUTES);
        List<Notification> notificationsToRetry = notificationRepository.findByStatusAndLastRetryAtBefore(
                NotificationStatus.PENDING_RETRY, retryBefore);

        if (notificationsToRetry.isEmpty()) {
            log.debug("No notifications to retry at this time");
            return;
        }

        log.info("Found {} notifications to retry", notificationsToRetry.size());

        for (Notification notification : notificationsToRetry) {
            retryNotification(notification);
        }
    }

    /**
     * Process a single scheduled notification
     */
    private void processScheduledNotification(Notification notification) {
        log.info("Processing scheduled notification: {}", notification.getId());

        try {
            // Update status from SCHEDULED to PENDING
            notification.setStatus(NotificationStatus.PENDING);
            notification = notificationRepository.save(notification);

            // Deliver the notification
            deliverNotification(notification);

            log.info("Successfully processed scheduled notification: {}", notification.getId());
        } catch (Exception e) {
            log.error("Error processing scheduled notification: {}", notification.getId(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage("Error processing scheduled notification: " + e.getMessage());
            notificationRepository.save(notification);
        }
    }

    /**
     * Retry a failed notification
     */
    private void retryNotification(Notification notification) {
        if (notification.getRetryCount() == null) {
            notification.setRetryCount(1);
        }

        log.info("Retrying notification: {} (attempt {}/{})",
                notification.getId(), notification.getRetryCount(), MAX_RETRY_ATTEMPTS);

        try {
            // Update status to PENDING
            notification.setStatus(NotificationStatus.PENDING);
            notification = notificationRepository.save(notification);

            // Attempt delivery again
            deliverNotification(notification);

            log.info("Successfully retried notification: {}", notification.getId());
        } catch (Exception e) {
            log.error("Error retrying notification: {}", notification.getId(), e);
            handleRetryFailure(notification, e);
        }
    }

    /**
     * Handle a failed retry attempt
     */
    private void handleRetryFailure(Notification notification, Exception e) {
        notification.setErrorMessage("Retry failed: " + e.getMessage());

        if (notification.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
            // Max retries reached, mark as permanently failed
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("Max retry attempts reached for notification: {}", notification.getId());
        } else {
            // Schedule another retry
            notification.setStatus(NotificationStatus.PENDING_RETRY);
            notification.setLastRetryAt(LocalDateTime.now());
            log.info("Scheduled for another retry, attempt {}/{}",
                    notification.getRetryCount(), MAX_RETRY_ATTEMPTS);
        }

        notificationRepository.save(notification);
    }

    /**
     * Deliver a notification based on its type
     */
    private void deliverNotification(Notification notification) {
        notification.setSentAt(LocalDateTime.now());

        try {
            NotificationChannel channel = notification.getType().getChannel();
            switch (channel) {
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
                    notificationRepository.save(notification);
                    break;
                default:
                    log.warn("Unknown channel for notification type: {}", notification.getType());
                    throw new IllegalArgumentException("Unknown notification channel: " + channel);
            }

            // If we get here without an exception, the notification was delivered successfully
            if (notification.getStatus() != NotificationStatus.DELIVERED) {
                notification.setStatus(NotificationStatus.DELIVERED);
                notification.setDeliveredAt(LocalDateTime.now());
                notificationRepository.save(notification);
            }
        } catch (Exception e) {
            throw new MessagingException("Failed to deliver notification: " + e.getMessage(), e);
        }
    }
}
