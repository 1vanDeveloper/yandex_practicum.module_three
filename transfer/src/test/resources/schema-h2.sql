-- Schema for transfer microservice (H2 version for tests)
CREATE SCHEMA IF NOT EXISTS transfer;

-- Table for storing money transfers between accounts
CREATE TABLE IF NOT EXISTS transfer.transfers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_account_login VARCHAR(255) NOT NULL,
    to_account_login VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster lookups by from_account_login
CREATE INDEX IF NOT EXISTS idx_transfers_from_login ON transfer.transfers(from_account_login);

-- Index for faster lookups by to_account_login
CREATE INDEX IF NOT EXISTS idx_transfers_to_login ON transfer.transfers(to_account_login);

-- Index for faster lookups by status
CREATE INDEX IF NOT EXISTS idx_transfers_status ON transfer.transfers(status);

-- Index for faster lookups by created_at (for ordering)
CREATE INDEX IF NOT EXISTS idx_transfers_created_at ON transfer.transfers(created_at);
