CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(255) NOT NULL,
    error_message VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    delivered_at DATETIME(6) NULL,
    is_read BIT(1) NOT NULL,
    retry_count INT NULL,
    last_retry_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    scheduled_at DATETIME(6) NULL,
    sent_at DATETIME(6) NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS notification_recipients (
    notification_id BIGINT NOT NULL,
    recipients VARCHAR(255) NULL,
    CONSTRAINT fk_notification_recipients_notification FOREIGN KEY (notification_id) REFERENCES notifications (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notification_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    notification_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    channel VARCHAR(255) NULL,
    recipient VARCHAR(255) NULL,
    subject VARCHAR(255) NULL,
    status VARCHAR(255) NULL,
    error_message VARCHAR(1000) NULL,
    retry_count INT NULL,
    timestamp DATETIME(6) NOT NULL,
    audit_timestamp DATETIME(6) NOT NULL,
    CONSTRAINT pk_notification_audit_log PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS notification_templates (
    id VARCHAR(255) NOT NULL,
    template_key VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    variables VARCHAR(255) NOT NULL,
    CONSTRAINT pk_notification_templates PRIMARY KEY (id),
    CONSTRAINT uk_notification_templates_template_key UNIQUE (template_key)
);

CREATE INDEX idx_notifications_entity_id ON notifications (entity_id);
CREATE INDEX idx_notifications_created_at ON notifications (created_at);
CREATE INDEX idx_notifications_status_last_retry ON notifications (status, last_retry_at);
CREATE INDEX idx_notifications_status_scheduled_at ON notifications (status, scheduled_at);
CREATE INDEX idx_notification_recipients_notification_id ON notification_recipients (notification_id);
CREATE INDEX idx_notification_recipients_recipient ON notification_recipients (recipients);
CREATE INDEX idx_notification_audit_notification_id ON notification_audit_log (notification_id);
CREATE INDEX idx_notification_audit_event_type ON notification_audit_log (event_type);
CREATE INDEX idx_notification_audit_recipient ON notification_audit_log (recipient);
CREATE INDEX idx_notification_audit_channel ON notification_audit_log (channel);
CREATE INDEX idx_notification_audit_timestamp ON notification_audit_log (timestamp);
