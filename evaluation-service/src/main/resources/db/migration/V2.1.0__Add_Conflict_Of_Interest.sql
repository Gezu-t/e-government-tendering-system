-- Conflict of Interest Declarations: Evaluators must declare conflicts before evaluating
CREATE TABLE IF NOT EXISTS conflict_of_interest_declarations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    evaluator_id BIGINT NOT NULL,
    has_conflict BIT(1) NOT NULL,
    conflict_description TEXT NULL,
    related_organization_id BIGINT NULL,
    relationship_type VARCHAR(100) NULL,
    declaration_text TEXT NOT NULL,
    acknowledged BIT(1) NOT NULL DEFAULT 0,
    reviewed_by BIGINT NULL,
    review_decision VARCHAR(30) NULL,
    review_comments TEXT NULL,
    reviewed_at DATETIME(6) NULL,
    declared_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_conflict_declarations PRIMARY KEY (id),
    CONSTRAINT uk_conflict_declarations UNIQUE (tender_id, evaluator_id)
);

CREATE INDEX idx_conflict_declarations_tender ON conflict_of_interest_declarations (tender_id);
CREATE INDEX idx_conflict_declarations_evaluator ON conflict_of_interest_declarations (evaluator_id);
CREATE INDEX idx_conflict_declarations_conflict ON conflict_of_interest_declarations (tender_id, has_conflict);
