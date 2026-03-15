CREATE TABLE IF NOT EXISTS evaluations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    bid_id BIGINT NOT NULL,
    evaluator_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    overall_score DECIMAL(10,2) NULL,
    comments TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_evaluations PRIMARY KEY (id),
    CONSTRAINT uk_evaluations_bid_evaluator UNIQUE (bid_id, evaluator_id)
);

CREATE TABLE IF NOT EXISTS criteria_scores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    evaluation_id BIGINT NOT NULL,
    criteria_id BIGINT NOT NULL,
    score DECIMAL(10,2) NOT NULL,
    justification TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_criteria_scores PRIMARY KEY (id),
    CONSTRAINT fk_criteria_scores_evaluation FOREIGN KEY (evaluation_id) REFERENCES evaluations (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS committee_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    committee_member_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    comments TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_committee_reviews PRIMARY KEY (id),
    CONSTRAINT uk_committee_reviews_tender_member UNIQUE (tender_id, committee_member_id)
);

CREATE TABLE IF NOT EXISTS tender_rankings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    bid_id BIGINT NOT NULL,
    final_score DECIMAL(10,2) NOT NULL,
    ranking_position INT NULL,
    is_winner BIT(1) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_tender_rankings PRIMARY KEY (id),
    CONSTRAINT uk_tender_rankings_tender_bid UNIQUE (tender_id, bid_id)
);

CREATE TABLE IF NOT EXISTS allocation_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    bid_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    total_price DECIMAL(15,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_allocation_results PRIMARY KEY (id)
);

CREATE INDEX idx_evaluations_tender_id ON evaluations (tender_id);
CREATE INDEX idx_evaluations_bid_id ON evaluations (bid_id);
CREATE INDEX idx_evaluations_evaluator_id ON evaluations (evaluator_id);
CREATE INDEX idx_evaluations_tender_status ON evaluations (tender_id, status);
CREATE INDEX idx_criteria_scores_evaluation_id ON criteria_scores (evaluation_id);
CREATE INDEX idx_criteria_scores_eval_criteria ON criteria_scores (evaluation_id, criteria_id);
CREATE INDEX idx_committee_reviews_tender_id ON committee_reviews (tender_id);
CREATE INDEX idx_committee_reviews_member_id ON committee_reviews (committee_member_id);
CREATE INDEX idx_committee_reviews_tender_status ON committee_reviews (tender_id, status);
CREATE INDEX idx_tender_rankings_tender_rank ON tender_rankings (tender_id, ranking_position);
CREATE INDEX idx_tender_rankings_tender_winner ON tender_rankings (tender_id, is_winner);
CREATE INDEX idx_allocation_results_tender_id ON allocation_results (tender_id);
CREATE INDEX idx_allocation_results_bid_id ON allocation_results (bid_id);
CREATE INDEX idx_allocation_results_tender_bid ON allocation_results (tender_id, bid_id);
