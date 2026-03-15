-- Digital Signatures: Non-repudiation for bids and contracts (Fong & Yan paper requirement)
CREATE TABLE IF NOT EXISTS digital_signatures (
    id BIGINT NOT NULL AUTO_INCREMENT,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    signer_id BIGINT NOT NULL,
    signer_name VARCHAR(255) NOT NULL,
    signature_value TEXT NOT NULL,
    content_hash VARCHAR(512) NOT NULL,
    algorithm VARCHAR(50) NOT NULL,
    certificate_serial VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL,
    signed_at DATETIME(6) NOT NULL,
    verified_at DATETIME(6) NULL,
    verified_by BIGINT NULL,
    rejection_reason VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_digital_signatures PRIMARY KEY (id)
);

CREATE INDEX idx_digital_signatures_entity ON digital_signatures (entity_type, entity_id);
CREATE INDEX idx_digital_signatures_signer ON digital_signatures (signer_id);
CREATE INDEX idx_digital_signatures_status ON digital_signatures (entity_type, entity_id, status);
CREATE UNIQUE INDEX uk_digital_signatures_entity_signer ON digital_signatures (entity_type, entity_id, signer_id);

-- Anti-Collusion: Track bid submission metadata for collusion detection
CREATE TABLE IF NOT EXISTS bid_submission_metadata (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bid_id BIGINT NOT NULL,
    tender_id BIGINT NOT NULL,
    tenderer_id BIGINT NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    device_fingerprint VARCHAR(255) NULL,
    geo_location VARCHAR(100) NULL,
    submission_time DATETIME(6) NOT NULL,
    session_id VARCHAR(255) NULL,
    flagged BIT(1) NULL DEFAULT 0,
    flag_reason VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_bid_submission_metadata PRIMARY KEY (id),
    CONSTRAINT fk_bid_submission_metadata_bid FOREIGN KEY (bid_id) REFERENCES bids (id) ON DELETE CASCADE
);

CREATE INDEX idx_bid_submission_metadata_tender ON bid_submission_metadata (tender_id);
CREATE INDEX idx_bid_submission_metadata_tenderer ON bid_submission_metadata (tenderer_id);
CREATE INDEX idx_bid_submission_metadata_ip ON bid_submission_metadata (tender_id, ip_address);
CREATE INDEX idx_bid_submission_metadata_device ON bid_submission_metadata (tender_id, device_fingerprint);
CREATE INDEX idx_bid_submission_metadata_flagged ON bid_submission_metadata (flagged);
