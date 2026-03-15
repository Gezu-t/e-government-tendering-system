package com.egov.tendering.notification.controller;

import com.egov.tendering.notification.dal.dto.NotificationRequest;
import com.egov.tendering.notification.dal.dto.NotificationResponse;
import com.egov.tendering.notification.dal.dto.NotificationSummaryDTO;
import com.egov.tendering.notification.dal.model.Notification;

import com.egov.tendering.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification API", description = "API for managing notifications")
public class NotificationController {

  private final NotificationService notificationService;

  @PostMapping
  @Operation(summary = "Send a new notification")
  @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
  public ResponseEntity<NotificationResponse> sendNotification(
          @Valid @RequestBody NotificationRequest request) {
    log.info("REST request to send notification: {}", request);
    NotificationResponse response = notificationService.sendNotification(request);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @GetMapping("/user/{userId}")
  @Operation(summary = "Get all notifications for a user")
  @PreAuthorize("hasRole('ADMIN') or @securityUtil.isCurrentUser(#userId)")
  public ResponseEntity<List<NotificationSummaryDTO>> getUserNotifications(
          @PathVariable String userId,
          @AuthenticationPrincipal Jwt jwt,
          @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
    log.info("REST request to get notifications for user: {}, unreadOnly: {}", userId, unreadOnly);

    Set<String> lookupIdentifiers = lookupIdentifiers(jwt, userId);
    List<Notification> notifications;
    if (unreadOnly) {
      notifications = isAdmin(jwt)
              ? notificationService.getUnreadNotificationsForUser(userId)
              : notificationService.getUnreadNotificationsForUsers(lookupIdentifiers);
    } else {
      notifications = isAdmin(jwt)
              ? notificationService.getNotificationsForUser(userId)
              : notificationService.getNotificationsForUsers(lookupIdentifiers);
    }

    // Map to DTOs
    List<NotificationSummaryDTO> dtos = notifications.stream()
            .map(this::mapToSummaryDto)
            .collect(Collectors.toList());

    return ResponseEntity.ok(dtos);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get notification details by ID")
  @PreAuthorize("hasRole('ADMIN') or @securityUtil.canAccessNotification(#id)")
  public ResponseEntity<Notification> getNotification(@PathVariable Long id) {
    log.info("REST request to get notification: {}", id);
    return notificationService.getNotification(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{id}/read")
  @Operation(summary = "Mark a notification as read")
  @PreAuthorize("hasRole('ADMIN') or @securityUtil.canAccessNotification(#id)")
  public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
    log.info("REST request to mark notification as read: {}", id);
    notificationService.markAsRead(id);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/count/unread/{userId}")
  @Operation(summary = "Count unread notifications for a user")
  @PreAuthorize("hasRole('ADMIN') or @securityUtil.isCurrentUser(#userId)")
  public ResponseEntity<Long> countUnread(
          @PathVariable String userId,
          @AuthenticationPrincipal Jwt jwt) {
    log.info("REST request to count unread notifications for user: {}", userId);
    long count = isAdmin(jwt)
            ? notificationService.countUnreadNotifications(userId)
            : notificationService.countUnreadNotifications(lookupIdentifiers(jwt, userId));
    return ResponseEntity.ok(count);
  }

  private Set<String> lookupIdentifiers(Jwt jwt, String requestedUserId) {
    Set<String> identifiers = new LinkedHashSet<>();
    if (jwt != null) {
      Object userIdClaim = jwt.getClaim("userId");
      if (userIdClaim != null && !userIdClaim.toString().isBlank()) {
        identifiers.add(userIdClaim.toString());
      }
      if (jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
        identifiers.add(jwt.getSubject());
      }
    }
    if (requestedUserId != null && !requestedUserId.isBlank()) {
      identifiers.add(requestedUserId);
    }
    return identifiers;
  }

  private boolean isAdmin(Jwt jwt) {
    if (jwt == null) {
      return false;
    }
    Object rolesClaim = jwt.getClaim("roles");
    if (rolesClaim instanceof Iterable<?> iterable) {
      for (Object role : iterable) {
        if ("ROLE_ADMIN".equals(String.valueOf(role))) {
          return true;
        }
      }
    }
    return rolesClaim != null && rolesClaim.toString().contains("ROLE_ADMIN");
  }

  private NotificationSummaryDTO mapToSummaryDto(Notification notification) {
    return new NotificationSummaryDTO(
            notification.getId(),
            notification.getType(),
            notification.getSubject(),
            notification.getEntityId(),
            notification.getCreatedAt(),
            notification.isRead()
    );
  }
}
