-- V1.0.0__Create_Document_Tables.sql

-- Create documents table
CREATE TABLE IF NOT EXISTS documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_extension VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    checksum VARCHAR(255),
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    description TEXT,
    version VARCHAR(20),
    is_public BOOLEAN DEFAULT FALSE,

    INDEX idx_document_entity (entity_type, entity_id),
    INDEX idx_document_type (document_type),
    INDEX idx_document_status (status),
    INDEX idx_document_created_by (created_by),
    INDEX idx_document_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create document_access_logs table
CREATE TABLE IF NOT EXISTS document_access_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    ip_address VARCHAR(50) NOT NULL,
    access_type VARCHAR(20) NOT NULL,
    user_agent VARCHAR(500),
    accessed_at TIMESTAMP NOT NULL,

    INDEX idx_access_document_id (document_id),
    INDEX idx_access_user_id (user_id),
    INDEX idx_access_accessed_at (accessed_at),

    CONSTRAINT fk_access_document
        FOREIGN KEY (document_id)
        REFERENCES documents (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create document_types lookup table for seeded types
CREATE TABLE IF NOT EXISTS document_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    allowed_extensions VARCHAR(255),

    CONSTRAINT uk_document_types_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert initial document types (if needed)
-- These are just examples, you might want to customize based on your needs
INSERT INTO document_types (name, description, allowed_extensions) VALUES
    ('TENDER_NOTICE', 'Official tender notice documents', 'pdf,doc,docx'),
    ('TENDER_DOCUMENT', 'Detailed tender documentation', 'pdf,doc,docx,xls,xlsx'),
    ('BID_DOCUMENT', 'Bid submission documents', 'pdf,doc,docx,xls,xlsx,zip'),
    ('CONTRACT_DOCUMENT', 'Contract and agreement documents', 'pdf,doc,docx'),
    ('TECHNICAL_SPECIFICATION', 'Technical requirements and specifications', 'pdf,doc,docx,xls,xlsx'),
    ('FINANCIAL_DOCUMENT', 'Financial statements and documents', 'pdf,doc,docx,xls,xlsx'),
    ('LEGAL_DOCUMENT', 'Legal certificates and documents', 'pdf,doc,docx'),
    ('CERTIFICATE', 'Certification documents', 'pdf,jpg,jpeg,png'),
    ('PROOF_OF_IDENTITY', 'Identity verification documents', 'pdf,jpg,jpeg,png'),
    ('PROOF_OF_ADDRESS', 'Address verification documents', 'pdf,jpg,jpeg,png'),
    ('BUSINESS_REGISTRATION', 'Business registration documents', 'pdf,doc,docx'),
    ('TAX_DOCUMENT', 'Tax-related documents', 'pdf,doc,docx,xls,xlsx'),
    ('SUPPLEMENTARY_DOCUMENT', 'Additional supporting documents', 'pdf,doc,docx,xls,xlsx,jpg,jpeg,png'),
    ('MISCELLANEOUS', 'Other document types', 'pdf,doc,docx,xls,xlsx,jpg,jpeg,png,zip');
