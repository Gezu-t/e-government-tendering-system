CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    username VARCHAR(255) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    entity_id VARCHAR(255) NULL,
    action VARCHAR(255) NOT NULL,
    details VARCHAR(4000) NULL,
    source_ip VARCHAR(255) NOT NULL,
    user_agent VARCHAR(255) NULL,
    success BIT(1) NOT NULL,
    failure_reason VARCHAR(255) NULL,
    event_type VARCHAR(255) NULL,
    description VARCHAR(255) NULL,
    module VARCHAR(255) NULL,
    sub_module VARCHAR(255) NULL,
    timestamp DATETIME(6) NOT NULL,
    correlation_id VARCHAR(255) NULL,
    service_id VARCHAR(255) NULL,
    host_name VARCHAR(255) NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action_type ON audit_logs (action_type);
CREATE INDEX idx_audit_logs_entity_type_entity_id ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs (timestamp);
CREATE INDEX idx_audit_logs_success ON audit_logs (success);
CREATE INDEX idx_audit_logs_correlation_id ON audit_logs (correlation_id);
