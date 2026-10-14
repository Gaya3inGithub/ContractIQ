-- ContractIQ schema for MySQL 8+
-- This script is safe to run repeatedly: it never drops tables or inserts demo
-- records. It creates an empty database for a clean local installation.

CREATE DATABASE IF NOT EXISTS contractiq_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE contractiq_db;

CREATE TABLE IF NOT EXISTS contracts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    stored_file_path VARCHAR(500),
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    file_hash_sha256 VARCHAR(64),
    contract_title VARCHAR(255) NOT NULL DEFAULT 'Untitled Contract',
    contract_type VARCHAR(100) DEFAULT 'General Agreement',
    party_first VARCHAR(255),
    party_second VARCHAR(255),
    effective_date DATE NULL,
    expiration_date DATE NULL,
    auto_renewal BOOLEAN NOT NULL DEFAULT FALSE,
    renewal_notice_period_days INT NULL,
    renewal_notice_deadline DATE NULL,
    renewal_terms TEXT,
    payment_terms TEXT,
    penalties_clause TEXT,
    termination_conditions TEXT,
    extraction_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    review_status VARCHAR(50) NOT NULL DEFAULT 'NEEDS_REVIEW',
    confidence_score DECIMAL(5,2) DEFAULT 0.00,
    review_notes TEXT,
    raw_text_excerpt MEDIUMTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contracts_review_status (review_status),
    INDEX idx_contracts_extraction_status (extraction_status),
    INDEX idx_contracts_expiration (expiration_date),
    INDEX idx_contracts_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS obligations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    obligation_title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    responsible_party VARCHAR(255) DEFAULT 'Unspecified',
    deadline_date DATE NULL,
    frequency VARCHAR(50) DEFAULT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    source_excerpt TEXT,
    page_number INT DEFAULT 1,
    confidence DECIMAL(5,2) DEFAULT 0.85,
    approval_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approved_by VARCHAR(100) NULL,
    review_comment TEXT NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_obligations_contract FOREIGN KEY (contract_id)
        REFERENCES contracts(id) ON DELETE CASCADE,
    INDEX idx_obligations_contract (contract_id),
    INDEX idx_obligations_approval (approval_status),
    INDEX idx_obligations_deadline (deadline_date),
    INDEX idx_obligations_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NULL,
    action_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    actor VARCHAR(100) NOT NULL DEFAULT 'System',
    ip_address VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_contract FOREIGN KEY (contract_id)
        REFERENCES contracts(id) ON DELETE SET NULL,
    INDEX idx_activity_created (created_at DESC),
    INDEX idx_activity_contract (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
