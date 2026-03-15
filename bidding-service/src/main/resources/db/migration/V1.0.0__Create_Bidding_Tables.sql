CREATE TABLE IF NOT EXISTS bids (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    tenderer_id BIGINT NOT NULL,
    contract_id BIGINT NULL,
    awarded_at DATETIME(6) NULL,
    award_comments VARCHAR(255) NULL,
    awarded_by BIGINT NULL,
    contract_created_at DATETIME(6) NULL,
    evaluated_by BIGINT NULL,
    evaluation_comments VARCHAR(255) NULL,
    evaluated_at DATETIME(6) NULL,
    status_reason VARCHAR(255) NULL,
    status VARCHAR(50) NOT NULL,
    total_price DECIMAL(15,2) NOT NULL,
    submission_time DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_bids PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS bid_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bid_id BIGINT NOT NULL,
    criteria_id BIGINT NOT NULL,
    value DECIMAL(15,2) NOT NULL,
    description TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_bid_items PRIMARY KEY (id),
    CONSTRAINT fk_bid_items_bid FOREIGN KEY (bid_id) REFERENCES bids (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bid_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bid_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_type VARCHAR(255) NULL,
    file_size BIGINT NULL,
    uploaded_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_bid_documents PRIMARY KEY (id),
    CONSTRAINT fk_bid_documents_bid FOREIGN KEY (bid_id) REFERENCES bids (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bid_clarifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bid_id BIGINT NOT NULL,
    question VARCHAR(255) NOT NULL,
    response VARCHAR(255) NULL,
    requested_by BIGINT NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    responded_at DATETIME(6) NULL,
    deadline DATETIME(6) NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT pk_bid_clarifications PRIMARY KEY (id),
    CONSTRAINT fk_bid_clarifications_bid FOREIGN KEY (bid_id) REFERENCES bids (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS compliance_requirements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    mandatory BIT(1) NOT NULL,
    type VARCHAR(50) NOT NULL,
    criteria_id BIGINT NULL,
    keyword VARCHAR(255) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_compliance_requirements PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS bid_compliance_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bid_id BIGINT NOT NULL,
    requirement_id BIGINT NULL,
    requirement VARCHAR(255) NOT NULL,
    mandatory BIT(1) NOT NULL,
    compliant BIT(1) NOT NULL,
    comment VARCHAR(255) NULL,
    verified_by BIGINT NULL,
    verified_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_bid_compliance_items PRIMARY KEY (id),
    CONSTRAINT fk_bid_compliance_items_bid FOREIGN KEY (bid_id) REFERENCES bids (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bid_securities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bid_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    issuer_name VARCHAR(255) NULL,
    reference_number VARCHAR(255) NULL,
    issue_date DATE NULL,
    expiry_date DATE NULL,
    document_path VARCHAR(255) NULL,
    status VARCHAR(50) NOT NULL,
    verified_by BIGINT NULL,
    verified_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_bid_securities PRIMARY KEY (id),
    CONSTRAINT uk_bid_securities_bid_id UNIQUE (bid_id),
    CONSTRAINT fk_bid_securities_bid FOREIGN KEY (bid_id) REFERENCES bids (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bid_versions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bid_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    version_data JSON NULL,
    change_summary VARCHAR(255) NULL,
    created_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_bid_versions PRIMARY KEY (id),
    CONSTRAINT uk_bid_versions_bid_version UNIQUE (bid_id, version_number)
);

CREATE TABLE IF NOT EXISTS bid_history (
    bid_id BIGINT NOT NULL,
    bid_submission_date DATETIME(6) NULL,
    bid_validity_period DATETIME(6) NULL,
    bid_amount DECIMAL(15,2) NULL,
    bid_currency VARCHAR(255) NULL,
    bidder_name VARCHAR(255) NULL,
    bidder_contact_details VARCHAR(255) NULL,
    bid_status VARCHAR(255) NULL,
    bid_feedback VARCHAR(255) NULL,
    awarded_bid BIT(1) NULL,
    contract_amount DECIMAL(15,2) NULL,
    contract_start_date DATETIME(6) NULL,
    contract_end_date DATETIME(6) NULL,
    CONSTRAINT pk_bid_history PRIMARY KEY (bid_id)
);

CREATE INDEX idx_bids_tender_id ON bids (tender_id);
CREATE INDEX idx_bids_tenderer_id ON bids (tenderer_id);
CREATE INDEX idx_bids_tender_status ON bids (tender_id, status);
CREATE INDEX idx_bid_items_bid_id ON bid_items (bid_id);
CREATE INDEX idx_bid_items_criteria_id ON bid_items (criteria_id);
CREATE INDEX idx_bid_documents_bid_id ON bid_documents (bid_id);
CREATE INDEX idx_bid_clarifications_bid_id ON bid_clarifications (bid_id);
CREATE INDEX idx_bid_clarifications_bid_status ON bid_clarifications (bid_id, status);
CREATE INDEX idx_compliance_requirements_tender_id ON compliance_requirements (tender_id);
CREATE INDEX idx_compliance_requirements_tender_mandatory ON compliance_requirements (tender_id, mandatory);
CREATE INDEX idx_bid_compliance_items_bid_id ON bid_compliance_items (bid_id);
CREATE INDEX idx_bid_compliance_items_requirement_id ON bid_compliance_items (requirement_id);
CREATE INDEX idx_bid_versions_bid_id ON bid_versions (bid_id);
