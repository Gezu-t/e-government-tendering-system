-- Multi-Criteria Evaluation: Configurable weighted scoring by category (Fong & Yan paper requirement)
-- Supports Technical, Financial, Compliance, Experience, and Quality scoring categories
-- with configurable weights and mandatory pass thresholds

CREATE TABLE IF NOT EXISTS evaluation_category_configs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tender_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    weight DECIMAL(5,2) NOT NULL,
    pass_threshold DECIMAL(5,2) NULL,
    mandatory BIT(1) NULL DEFAULT 0,
    description VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_evaluation_category_configs PRIMARY KEY (id)
);

CREATE INDEX idx_eval_category_configs_tender ON evaluation_category_configs (tender_id);
CREATE UNIQUE INDEX uk_eval_category_configs ON evaluation_category_configs (tender_id, category);

CREATE TABLE IF NOT EXISTS evaluation_score_summaries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    evaluation_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    category_weight DECIMAL(5,2) NOT NULL,
    raw_score DECIMAL(10,2) NOT NULL,
    weighted_score DECIMAL(10,2) NOT NULL,
    max_possible_score DECIMAL(10,2) NULL,
    criteria_count INT NULL,
    pass_threshold DECIMAL(5,2) NULL,
    passed BIT(1) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_evaluation_score_summaries PRIMARY KEY (id),
    CONSTRAINT fk_eval_score_summaries_eval FOREIGN KEY (evaluation_id) REFERENCES evaluations (id) ON DELETE CASCADE
);

CREATE INDEX idx_eval_score_summaries_eval ON evaluation_score_summaries (evaluation_id);
CREATE UNIQUE INDEX uk_eval_score_summaries ON evaluation_score_summaries (evaluation_id, category);
