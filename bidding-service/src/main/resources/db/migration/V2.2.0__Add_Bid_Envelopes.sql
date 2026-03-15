-- Two-Envelope Bid System: Separates technical and financial proposals
-- Technical envelope is opened first; financial envelope only opened for technically qualified bids
CREATE TABLE IF NOT EXISTS bid_envelopes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bid_id BIGINT NOT NULL,
    envelope_type VARCHAR(30) NOT NULL,
    content LONGTEXT NULL,
    content_hash VARCHAR(512) NULL,
    is_sealed BIT(1) NOT NULL DEFAULT 1,
    opened_at DATETIME(6) NULL,
    opened_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_bid_envelopes PRIMARY KEY (id),
    CONSTRAINT uk_bid_envelopes UNIQUE (bid_id, envelope_type),
    CONSTRAINT fk_bid_envelopes_bid FOREIGN KEY (bid_id) REFERENCES bids (id) ON DELETE CASCADE
);

CREATE INDEX idx_bid_envelopes_bid ON bid_envelopes (bid_id);
CREATE INDEX idx_bid_envelopes_sealed ON bid_envelopes (bid_id, is_sealed);
