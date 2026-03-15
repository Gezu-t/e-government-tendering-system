package com.egov.tendering.notification.service;

import com.egov.tendering.notification.dal.model.Notification;
import com.egov.tendering.notification.dal.model.NotificationStatus;
import com.egov.tendering.notification.dal.repository.NotificationRepository;
import com.egov.tendering.notification.event.NotificationEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateService templateService;
    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher eventPublisher;

    private static final int MAX_RETRY_ATTEMPTS = 3;

    public void sendEmail(Notification notification) {
        try {
            log.info("Sending email notification: {}", notification.getId());

            if (notification.getRetryCount() == null) {
                notification.setRetryCount(0);
            }

            for (String recipient : notification.getRecipients()) {
                sendEmailToRecipient(notification, recipient);
            }

            // Update notification status
            notification.setStatus(NotificationStatus.DELIVERED);
            notification.setDeliveredAt(LocalDateTime.now());
            notificationRepository.save(notification);

            // Publish event
            eventPublisher.publishEmailSentEvent(notification);
        } catch (Exception e) {
            handleSendingError(notification, e);
        }
    }

    private void sendEmailToRecipient(Notification notification, String recipient) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Set email parameters
        helper.setTo(recipient);
        helper.setSubject(notification.getSubject());

        // Process the template if needed
        String emailContent = templateService.processTemplate(
                notification.getType().name().toLowerCase() + "_email",
                notification
        );

        helper.setText(emailContent, true);

        // Send the email
        mailSender.send(message);
        log.info("Email sent to: {}", recipient);
    }

    private void handleSendingError(Notification notification, Exception exception) {
        log.error("Failed to send email notification: {}", notification.getId(), exception);

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
}
