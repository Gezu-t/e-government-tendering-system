CREATE TABLE IF NOT EXISTS tenders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    tenderee_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    submission_deadline DATETIME(6) NOT NULL,
    allocation_strategy VARCHAR(50) NOT NULL,
    min_winners INT NULL,
    max_winners INT NULL,
    cutoff_score DECIMAL(10,2) NULL,
    is_average_allocation BIT(1) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_tenders PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pricing_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    default_price DECIMAL(15,2) NULL,
    unit VARCHAR(20) NULL,
    created_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_pricing_items PRIMARY KEY (id),
    CONSTRAINT uk_pricing_items_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS tender_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(255) NULL,
    type VARCHAR(255) NULL,
    category_description VARCHAR(255) NULL,
    active BIT(1) NOT NULL,
    tender_id BIGINT NULL,
    CONSTRAINT pk_tender_category PRIMARY KEY (id),
    CONSTRAINT fk_tender_category_tender FOREIGN KEY (tender_id) REFERENCES tenders (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tender_criteria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    type VARCHAR(50) NOT NULL,
    weight DECIMAL(5,2) NOT NULL,
    prefer_higher BIT(1) NULL,
    active BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_tender_criteria PRIMARY KEY (id),
    CONSTRAINT fk_tender_criteria_tender FOREIGN KEY (tender_id) REFERENCES tenders (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tender_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    criteria_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    quantity INT NOT NULL,
    unit VARCHAR(255) NULL,
    estimated_price DECIMAL(15,2) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_tender_items PRIMARY KEY (id),
    CONSTRAINT fk_tender_items_tender FOREIGN KEY (tender_id) REFERENCES tenders (id) ON DELETE CASCADE,
    CONSTRAINT fk_tender_items_criteria FOREIGN KEY (criteria_id) REFERENCES tender_criteria (id)
);

CREATE INDEX idx_tenders_tenderee_id ON tenders (tenderee_id);
CREATE INDEX idx_tenders_status ON tenders (status);
CREATE INDEX idx_tenders_status_type ON tenders (status, type);
CREATE INDEX idx_tenders_submission_deadline ON tenders (submission_deadline);
CREATE INDEX idx_tender_category_name ON tender_category (category_name);
CREATE INDEX idx_tender_category_active ON tender_category (active);
CREATE INDEX idx_tender_category_tender_id ON tender_category (tender_id);
CREATE INDEX idx_tender_criteria_tender_id ON tender_criteria (tender_id);
CREATE INDEX idx_tender_items_tender_id ON tender_items (tender_id);
CREATE INDEX idx_tender_items_criteria_id ON tender_items (criteria_id);
