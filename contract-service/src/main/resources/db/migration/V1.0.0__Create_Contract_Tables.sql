CREATE TABLE IF NOT EXISTS contracts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    contract_number VARCHAR(255) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_value DECIMAL(38,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50) NULL,
    version BIGINT NULL,
    CONSTRAINT pk_contracts PRIMARY KEY (id),
    CONSTRAINT uk_contracts_contract_number UNIQUE (contract_number)
);

CREATE TABLE IF NOT EXISTS contract_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contract_id BIGINT NOT NULL,
    tender_item_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    quantity INT NOT NULL,
    unit VARCHAR(255) NOT NULL,
    unit_price DECIMAL(38,2) NOT NULL,
    total_price DECIMAL(38,2) NOT NULL,
    CONSTRAINT pk_contract_items PRIMARY KEY (id),
    CONSTRAINT fk_contract_items_contract FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS contract_milestones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contract_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    due_date DATE NOT NULL,
    payment_amount DECIMAL(38,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    completed_date DATE NULL,
    CONSTRAINT pk_contract_milestones PRIMARY KEY (id),
    CONSTRAINT fk_contract_milestones_contract FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
);

CREATE INDEX idx_contracts_tender_bidder ON contracts (tender_id, bidder_id);
CREATE INDEX idx_contracts_contract_number ON contracts (contract_number);
CREATE INDEX idx_contracts_bidder_id ON contracts (bidder_id);
CREATE INDEX idx_contracts_status_end_date ON contracts (status, end_date);
CREATE INDEX idx_contract_items_contract_id ON contract_items (contract_id);
CREATE INDEX idx_contract_milestones_contract_id ON contract_milestones (contract_id);
CREATE INDEX idx_contract_milestones_status_due_date ON contract_milestones (status, due_date);
