package com.egov.tendering.notification.config;

import com.egov.tendering.notification.service.SmsService.SmsSender;
import com.egov.tendering.notification.service.PushNotificationService.PushSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Notification channel configuration.
 *
 * Email: Spring Boot auto-configures JavaMailSender from spring.mail.* properties.
 *        No explicit bean is needed here — define SMTP_HOST, SMTP_USERNAME, SMTP_PASSWORD
 *        as environment variables (see application.yml).
 *
 * SMS / Push: dev/test use explicit mock logging beans. Other profiles receive
 *             no-op defaults so the service can still start until real provider
 *             integrations are added.
 */
@Configuration
@Slf4j
public class NotificationConfig {

    @Bean
    @Profile({"dev", "test"})
    public SmsSender mockSmsSender() {
        return (phoneNumber, message) ->
                log.info("[MOCK SMS] to={} message={}", phoneNumber, message);
    }

    @Bean
    @Profile("!dev & !test")
    public SmsSender defaultSmsSender() {
        return (phoneNumber, message) ->
                log.warn("SMS provider not configured. Skipping send to={}", phoneNumber);
    }

    @Bean
    @Profile({"dev", "test"})
    public PushSender mockPushSender() {
        return (userId, title, body, data) ->
                log.info("[MOCK PUSH] userId={} title={} body={} data={}", userId, title, body, data);
    }

    @Bean
    @Profile("!dev & !test")
    public PushSender defaultPushSender() {
        return (userId, title, body, data) ->
                log.warn("Push provider not configured. Skipping send to userId={}", userId);
    }
}
