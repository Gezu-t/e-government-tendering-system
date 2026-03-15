-- Organization Blacklist/Debarment: Prevents disqualified vendors from bidding
CREATE TABLE IF NOT EXISTS organization_blacklist (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    reason TEXT NOT NULL,
    reference_number VARCHAR(100) NULL,
    effective_from DATE NOT NULL,
    effective_until DATE NULL,
    is_permanent BIT(1) NOT NULL DEFAULT 0,
    imposed_by BIGINT NOT NULL,
    lifted_by BIGINT NULL,
    lifted_at DATETIME(6) NULL,
    lift_reason VARCHAR(255) NULL,
    active BIT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_organization_blacklist PRIMARY KEY (id),
    CONSTRAINT fk_org_blacklist_org FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE
);

CREATE INDEX idx_org_blacklist_org ON organization_blacklist (organization_id, active);
CREATE INDEX idx_org_blacklist_active ON organization_blacklist (active, effective_until);
