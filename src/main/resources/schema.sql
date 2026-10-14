-- ==============================================================================
-- CONTRACTIQ DATABASE SCHEMA (MySQL 8+)
-- Complete schema for contracts, obligations, review metadata, and activity logs
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS contractiq_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE contractiq_db;

-- Drop in correct order of foreign keys
DROP TABLE IF EXISTS activity_logs;
DROP TABLE IF EXISTS obligations;
DROP TABLE IF EXISTS contracts;

-- 1. Contracts Table
CREATE TABLE contracts (
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
    
    extraction_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, EXTRACTING, EXTRACTED, FAILED
    review_status VARCHAR(50) NOT NULL DEFAULT 'NEEDS_REVIEW', -- NEEDS_REVIEW, PARTIALLY_APPROVED, APPROVED, REJECTED
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

-- 2. Obligations Table
CREATE TABLE obligations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    obligation_title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    responsible_party VARCHAR(255) DEFAULT 'Unspecified',
    deadline_date DATE NULL,
    frequency VARCHAR(50) DEFAULT 'ONE_TIME', -- ONE_TIME, MONTHLY, QUARTERLY, ANNUALLY, AS_INCURRED
    category VARCHAR(50) NOT NULL DEFAULT 'OTHER', -- PAYMENT, COMPLIANCE, DELIVERY, REPORTING, TERMINATION, CONFIDENTIALITY, OTHER
    
    source_excerpt TEXT,
    page_number INT DEFAULT 1,
    confidence DECIMAL(5,2) DEFAULT 0.85,
    
    -- MANDATORY HUMAN APPROVAL WORKFLOW
    -- AI-generated suggestions ALWAYS start as PENDING. 
    -- Human reviewer can EDIT, APPROVE, or REJECT.
    approval_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
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

-- 3. Activity Logs Table
CREATE TABLE activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NULL,
    action_type VARCHAR(100) NOT NULL, -- CONTRACT_UPLOADED, TEXT_EXTRACTED, AI_ANALYSIS_COMPLETED, OBLIGATION_APPROVED, OBLIGATION_REJECTED, OBLIGATION_UPDATED, CONTRACT_STATUS_CHANGED
    description TEXT NOT NULL,
    actor VARCHAR(100) NOT NULL DEFAULT 'System',
    ip_address VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_activity_contract FOREIGN KEY (contract_id) 
        REFERENCES contracts(id) ON DELETE SET NULL,
    INDEX idx_activity_created (created_at DESC),
    INDEX idx_activity_contract (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Synthetic Seed Data for Immediate Testing & Verification
INSERT INTO contracts (
    id, file_name, stored_file_path, file_size_bytes, contract_title, contract_type,
    party_first, party_second, effective_date, expiration_date, auto_renewal,
    renewal_notice_period_days, renewal_notice_deadline, renewal_terms,
    payment_terms, penalties_clause, termination_conditions, extraction_status, review_status,
    confidence_score, review_notes, raw_text_excerpt
) VALUES (
    1, 'CloudScale_Enterprise_SaaS_2025.pdf', 'uploads/CloudScale_Enterprise_SaaS_2025.pdf', 145820,
    'Enterprise Cloud Services Agreement', 'SaaS Agreement',
    'CloudScale Infrastructure Inc.', 'Apex Global Logistics LLC',
    '2025-01-01', '2026-01-01', TRUE, 60, '2025-11-02',
    'Renews automatically for consecutive 12-month periods unless written termination notice is given at least 60 days prior to expiry.',
    'Net 30 days from invoice receipt via wire transfer. Annual fee $120,000 payable quarterly.',
    '1.5% late fee per month on overdue balances exceeding 15 days.',
    'Either party may terminate for convenience with 90 days written notice or immediately for material uncured breach within 30 days.',
    'EXTRACTED', 'PARTIALLY_APPROVED', 0.94, 'Initial human review completed; 2 obligations approved, 1 pending review.',
    'This Enterprise Cloud Services Agreement ("Agreement") is made between CloudScale Infrastructure Inc. and Apex Global Logistics LLC...'
);

INSERT INTO obligations (
    id, contract_id, obligation_title, description, responsible_party,
    deadline_date, frequency, category, source_excerpt, page_number,
    confidence, approval_status, approved_by, review_comment, reviewed_at
) VALUES 
(
    1, 1, 'Quarterly Platform Subscription Payment', 
    'Customer shall pay the quarterly platform subscription fee of $30,000 within thirty (30) days of receiving quarterly invoice.', 
    'Apex Global Logistics LLC', '2025-04-01', 'QUARTERLY', 'PAYMENT',
    'Section 4.1: Customer shall pay quarterly invoices Net 30 from invoice receipt.', 3, 0.98,
    'APPROVED', 'Senior Legal Counsel', 'Verified against pricing schedule addendum A', CURRENT_TIMESTAMP
),
(
    2, 1, 'Written Renewal / Non-Renewal Notice', 
    'Written notice of intent not to auto-renew must be delivered at least sixty (60) days prior to the expiration date.', 
    'Apex Global Logistics LLC', '2025-11-02', 'ONE_TIME', 'TERMINATION',
    'Section 8.2: Written notice must be served 60 days before expiration to prevent automatic renewal.', 6, 0.95,
    'APPROVED', 'Contract Operations Manager', 'Flagged in calendar for executive renewal review', CURRENT_TIMESTAMP
),
(
    3, 1, 'Annual SOC 2 Type II Compliance Audit Report', 
    'Provider shall furnish an updated SOC 2 Type II audit report to Customer annually within thirty (30) days of certification completion.', 
    'CloudScale Infrastructure Inc.', '2025-06-30', 'ANNUALLY', 'COMPLIANCE',
    'Section 12.3: Provider shall provide SOC 2 Type II reports within 30 days of annual audit completion.', 9, 0.91,
    'PENDING', NULL, NULL, NULL
);

INSERT INTO activity_logs (contract_id, action_type, description, actor) VALUES
(1, 'CONTRACT_UPLOADED', 'Contract PDF "CloudScale_Enterprise_SaaS_2025.pdf" successfully uploaded and stored.', 'Legal Admin'),
(1, 'TEXT_EXTRACTED', 'Apache PDFBox extracted 12 pages (18,420 characters) from contract document.', 'System'),
(1, 'AI_ANALYSIS_COMPLETED', 'AI extraction completed with 94% confidence score; 3 obligations identified.', 'ContractIQ AI Engine'),
(1, 'OBLIGATION_APPROVED', 'Obligation #1 "Quarterly Platform Subscription Payment" approved by Senior Legal Counsel.', 'Senior Legal Counsel'),
(1, 'OBLIGATION_APPROVED', 'Obligation #2 "Written Renewal / Non-Renewal Notice" approved by Contract Operations Manager.', 'Contract Operations Manager');
