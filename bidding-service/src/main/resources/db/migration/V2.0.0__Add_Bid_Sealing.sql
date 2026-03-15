-- Bid Sealing: Implements sealed bidding as per Fong & Yan paper
-- Bids are encrypted upon submission and remain sealed until the tender deadline passes
CREATE TABLE IF NOT EXISTS bid_seals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bid_id BIGINT NOT NULL,
    tender_id BIGINT NOT NULL,
    content_hash VARCHAR(512) NOT NULL,
    encrypted_content LONGTEXT NULL,
    encryption_algorithm VARCHAR(50) NOT NULL,
    seal_key_reference VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL,
    sealed_at DATETIME(6) NOT NULL,
    sealed_by BIGINT NOT NULL,
    unsealed_at DATETIME(6) NULL,
    unsealed_by BIGINT NULL,
    scheduled_unseal_time DATETIME(6) NOT NULL,
    integrity_verified BIT(1) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_bid_seals PRIMARY KEY (id),
    CONSTRAINT uk_bid_seals_bid_id UNIQUE (bid_id),
    CONSTRAINT fk_bid_seals_bid FOREIGN KEY (bid_id) REFERENCES bids (id) ON DELETE CASCADE
);

CREATE INDEX idx_bid_seals_tender_id ON bid_seals (tender_id);
CREATE INDEX idx_bid_seals_status ON bid_seals (status);
CREATE INDEX idx_bid_seals_scheduled_unseal ON bid_seals (status, scheduled_unseal_time);
