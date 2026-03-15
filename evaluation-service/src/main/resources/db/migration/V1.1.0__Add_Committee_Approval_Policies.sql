CREATE TABLE IF NOT EXISTS committee_approval_policies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    required_review_count INT NOT NULL,
    minimum_approval_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_committee_approval_policies PRIMARY KEY (id),
    CONSTRAINT uk_committee_approval_policies_tender UNIQUE (tender_id)
);

CREATE INDEX idx_committee_approval_policies_tender_id ON committee_approval_policies (tender_id);
