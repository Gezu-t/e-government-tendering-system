-- Tender Amendments: Track amendments to published tenders (Fong & Yan paper requirement)
-- Ensures all bidders are notified when tender terms change
CREATE TABLE IF NOT EXISTS tender_amendments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    amendment_number INT NOT NULL,
    reason TEXT NOT NULL,
    description TEXT NULL,
    previous_deadline DATETIME(6) NULL,
    new_deadline DATETIME(6) NULL,
    previous_description TEXT NULL,
    amended_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_tender_amendments PRIMARY KEY (id),
    CONSTRAINT uk_tender_amendments_number UNIQUE (tender_id, amendment_number),
    CONSTRAINT fk_tender_amendments_tender FOREIGN KEY (tender_id) REFERENCES tenders (id) ON DELETE CASCADE
);

CREATE INDEX idx_tender_amendments_tender_id ON tender_amendments (tender_id);
