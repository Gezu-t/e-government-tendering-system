-- Vendor Performance Scorecard: Post-award performance monitoring per Fong & Yan paper
CREATE TABLE IF NOT EXISTS vendor_performances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contract_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    tender_id BIGINT NOT NULL,
    quality_score DECIMAL(5,2) NULL,
    timeliness_score DECIMAL(5,2) NULL,
    compliance_score DECIMAL(5,2) NULL,
    communication_score DECIMAL(5,2) NULL,
    overall_score DECIMAL(5,2) NULL,
    milestones_completed INT NULL,
    milestones_total INT NULL,
    milestones_on_time INT NULL,
    penalties_count INT NULL,
    penalty_amount DECIMAL(15,2) NULL,
    review_comments TEXT NULL,
    reviewed_by BIGINT NULL,
    review_period VARCHAR(50) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_vendor_performances PRIMARY KEY (id),
    CONSTRAINT fk_vendor_perf_contract FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
);

CREATE INDEX idx_vendor_perf_contract ON vendor_performances (contract_id);
CREATE INDEX idx_vendor_perf_vendor ON vendor_performances (vendor_id);

-- Contract Amendments: Track changes to active contracts with approval workflow
CREATE TABLE IF NOT EXISTS contract_amendments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contract_id BIGINT NOT NULL,
    amendment_number INT NOT NULL,
    type VARCHAR(30) NOT NULL,
    reason TEXT NOT NULL,
    description TEXT NULL,
    previous_value DECIMAL(15,2) NULL,
    new_value DECIMAL(15,2) NULL,
    previous_end_date DATE NULL,
    new_end_date DATE NULL,
    status VARCHAR(30) NOT NULL,
    requested_by BIGINT NOT NULL,
    approved_by BIGINT NULL,
    approved_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_contract_amendments PRIMARY KEY (id),
    CONSTRAINT uk_contract_amendments UNIQUE (contract_id, amendment_number),
    CONSTRAINT fk_contract_amend_contract FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
);

CREATE INDEX idx_contract_amendments_contract ON contract_amendments (contract_id);
CREATE INDEX idx_contract_amendments_status ON contract_amendments (contract_id, status);
