package com.egov.tendering.notification.dal.dto;

import com.egov.tendering.notification.dal.model.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private NotificationStatus status;
    private String message;
}
