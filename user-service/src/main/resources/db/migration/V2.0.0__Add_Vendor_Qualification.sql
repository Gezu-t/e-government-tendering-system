-- Vendor Pre-Qualification: Vendors must be pre-qualified before bidding (Fong & Yan paper requirement)
-- Ensures only verified, capable vendors can participate in government tenders
CREATE TABLE IF NOT EXISTS vendor_qualifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    qualification_category VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    business_license_number VARCHAR(100) NULL,
    tax_registration_number VARCHAR(100) NULL,
    years_of_experience INT NULL,
    annual_revenue VARCHAR(50) NULL,
    employee_count INT NULL,
    past_contracts_count INT NULL,
    certification_details TEXT NULL,
    financial_statement_path VARCHAR(500) NULL,
    qualification_score INT NULL,
    reviewer_id BIGINT NULL,
    review_comments TEXT NULL,
    reviewed_at DATETIME(6) NULL,
    valid_from DATE NULL,
    valid_until DATE NULL,
    rejection_reason VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_vendor_qualifications PRIMARY KEY (id),
    CONSTRAINT fk_vendor_qualifications_org FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE
);

CREATE INDEX idx_vendor_qualifications_org ON vendor_qualifications (organization_id);
CREATE INDEX idx_vendor_qualifications_status ON vendor_qualifications (status);
CREATE INDEX idx_vendor_qualifications_org_category ON vendor_qualifications (organization_id, qualification_category);
CREATE INDEX idx_vendor_qualifications_valid_until ON vendor_qualifications (status, valid_until);
