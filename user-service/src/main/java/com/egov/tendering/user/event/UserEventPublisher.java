package com.egov.tendering.user.event;

import com.egov.tendering.user.dal.model.Organization;
import com.egov.tendering.user.dal.model.OrganizationStatus;
import com.egov.tendering.user.dal.model.User;
import com.egov.tendering.user.dal.model.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.user-events}")
    private String userEventsTopic;

    public void publishUserCreatedEvent(User user) {
        UserCreatedEvent event = UserCreatedEvent.builder()
                .eventId(ThreadLocalRandom.current().nextLong())
                .eventType("USER_CREATED")
                .timestamp(LocalDateTime.now())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

        log.info("Publishing user created event: {}", event);
        sendEvent(user.getId().toString(), event);
    }

    public void publishUserStatusChangedEvent(User user, UserStatus oldStatus) {
        UserStatusChangedEvent event = UserStatusChangedEvent.builder()
                .eventId(ThreadLocalRandom.current().nextLong())
                .eventType("USER_STATUS_CHANGED")
                .timestamp(LocalDateTime.now())
                .userId(user.getId())
                .username(user.getUsername())
                .oldStatus(oldStatus)
                .newStatus(user.getStatus())
                .build();

        log.info("Publishing user status changed event: {}", event);
        sendEvent(user.getId().toString(), event);
    }

    public void publishUserDeletedEvent(User user) {
        UserDeletedEvent event = UserDeletedEvent.builder()
                .eventId(ThreadLocalRandom.current().nextLong())
                .eventType("USER_DELETED")
                .timestamp(LocalDateTime.now())
                .userId(user.getId())
                .username(user.getUsername())
                .build();

        log.info("Publishing user deleted event: {}", event);
        sendEvent(user.getId().toString(), event);
    }

    public void publishUserLoginEvent(User user) {
        UserLoginEvent event = UserLoginEvent.builder()
                .eventId(ThreadLocalRandom.current().nextLong())
                .eventType("USER_LOGIN")
                .timestamp(LocalDateTime.now())
                .userId(user.getId())
                .username(user.getUsername())
                .build();

        log.info("Publishing user login event: {}", event);
        sendEvent(user.getId().toString(), event);
    }

    public void publishOrganizationCreatedEvent(Organization organization) {
        OrganizationCreatedEvent event = OrganizationCreatedEvent.builder()
                .eventId(ThreadLocalRandom.current().nextLong())
                .eventType("ORGANIZATION_CREATED")
                .timestamp(LocalDateTime.now())
                .organizationId(organization.getId())
                .organizationName(organization.getName())
                .registrationNumber(organization.getRegistrationNumber())
                .organizationType(organization.getOrganizationType())
                .build();

        log.info("Publishing organization created event: {}", event);
        sendEvent(organization.getId().toString(), event);
    }

    public void publishOrganizationStatusChangedEvent(Organization organization, OrganizationStatus oldStatus) {
        OrganizationStatusChangedEvent event = OrganizationStatusChangedEvent.builder()
                .eventId(ThreadLocalRandom.current().nextLong())
                .eventType("ORGANIZATION_STATUS_CHANGED")
                .timestamp(LocalDateTime.now())
                .organizationId(organization.getId())
                .organizationName(organization.getName())
                .oldStatus(oldStatus)
                .newStatus(organization.getStatus())
                .build();

        log.info("Publishing organization status changed event: {}", event);
        sendEvent(organization.getId().toString(), event);
    }

    public void publishOrganizationDeletedEvent(Organization organization) {
        OrganizationDeletedEvent event = OrganizationDeletedEvent.builder()
                .eventId(ThreadLocalRandom.current().nextLong())
                .eventType("ORGANIZATION_DELETED")
                .timestamp(LocalDateTime.now())
                .organizationId(organization.getId())
                .organizationName(organization.getName())
                .build();

        log.info("Publishing organization deleted event: {}", event);
        sendEvent(organization.getId().toString(), event);
    }

    public void publishUserOrganizationChangedEvent(User user, Organization organization, String action, String role) {
        UserOrganizationChangedEvent event = UserOrganizationChangedEvent.builder()
                .eventId(ThreadLocalRandom.current().nextLong())
                .eventType("USER_ORGANIZATION_CHANGED")
                .timestamp(LocalDateTime.now())
                .userId(user.getId())
                .username(user.getUsername())
                .organizationId(organization.getId())
                .organizationName(organization.getName())
                .action(action)
                .role(role)
                .build();

        log.info("Publishing user organization changed event: {}", event);
        sendEvent(user.getId().toString(), event);
    }

    private void sendEvent(String key, Object event) {
        // Run on a separate thread so kafkaTemplate.send() — which can block up to
        // max.block.ms waiting for broker metadata — never blocks the servlet thread.
        CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(userEventsTopic, key, event)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to send event to topic {}: {}", userEventsTopic, ex.getMessage());
                            } else {
                                log.debug("Successfully sent event to topic {} with offset {}",
                                        userEventsTopic, result.getRecordMetadata().offset());
                            }
                        });
            } catch (Exception ex) {
                log.error("Failed to dispatch event to topic {}: {}", userEventsTopic, ex.getMessage());
            }
        });
    }
}