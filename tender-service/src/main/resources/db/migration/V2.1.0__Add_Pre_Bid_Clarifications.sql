-- Pre-Bid Clarifications: Q&A between vendors and tenderee before bid submission
-- Supports public Q&A visible to all vendors (transparency per Fong & Yan paper)
CREATE TABLE IF NOT EXISTS pre_bid_clarifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NULL,
    asked_by BIGINT NOT NULL,
    asked_by_org_name VARCHAR(200) NULL,
    answered_by BIGINT NULL,
    category VARCHAR(100) NULL,
    is_public BIT(1) NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL,
    asked_at DATETIME(6) NOT NULL,
    answered_at DATETIME(6) NULL,
    CONSTRAINT pk_pre_bid_clarifications PRIMARY KEY (id),
    CONSTRAINT fk_pre_bid_clarifications_tender FOREIGN KEY (tender_id) REFERENCES tenders (id) ON DELETE CASCADE
);

CREATE INDEX idx_pre_bid_clarifications_tender ON pre_bid_clarifications (tender_id);
CREATE INDEX idx_pre_bid_clarifications_status ON pre_bid_clarifications (tender_id, status);
CREATE INDEX idx_pre_bid_clarifications_asked_by ON pre_bid_clarifications (tender_id, asked_by);
CREATE INDEX idx_pre_bid_clarifications_public ON pre_bid_clarifications (tender_id, is_public, status);
