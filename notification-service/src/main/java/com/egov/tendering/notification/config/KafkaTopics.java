package com.egov.tendering.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kafka.topics")
@Data
public class KafkaTopics {
    // Notification events
    private String notificationSent = "notification-sent";
    private String notificationEvents = "notification-events";

    // Tender events
    private String tenderCreated = "tender-created";
    private String tenderUpdated = "tender-updated";
    private String tenderPublished = "tender-published";
    private String tenderClosed = "tender-closed";
    private String tenderCancelled = "tender-cancelled";

    // Bidding events
    private String bidSubmitted = "bid-submitted";
    private String bidUpdated = "bid-updated";
    private String bidWithdrawn = "bid-withdrawn";
    private String bidEvaluationCompleted = "bid-evaluation-completed";

    // Contract events
    private String contractAwarded = "contract-awarded";
    private String contractSigned = "contract-signed";
    private String contractCancelled = "contract-cancelled";

    // User events
    private String userRegistered = "user-registered";
}
