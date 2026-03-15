package com.egov.tendering.notification.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class NotificationFailedEvent extends NotificationEvent {
    private String channel;
    private String recipient;
    private Long recipientUserId;
    private String errorMessage;
    private Integer retryCount;
}
