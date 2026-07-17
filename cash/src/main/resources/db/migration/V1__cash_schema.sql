-- V1__cash_schema.sql
-- Schema for cash microservice

CREATE SCHEMA IF NOT EXISTS cash;

-- Table for storing cash transactions (deposits and withdrawals)
CREATE TABLE IF NOT EXISTS cash.cash_transactions (
    id BIGSERIAL PRIMARY KEY,
    account_login VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster lookups by account login
CREATE INDEX IF NOT EXISTS idx_cash_transactions_login ON cash.cash_transactions(account_login);

-- Index for faster lookups by status
CREATE INDEX IF NOT EXISTS idx_cash_transactions_status ON cash.cash_transactions(status);

-- Index for faster lookups by created_at (for ordering)
CREATE INDEX IF NOT EXISTS idx_cash_transactions_created_at ON cash.cash_transactions(created_at);

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION cash.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_cash_transactions_updated_at
    BEFORE UPDATE ON cash.cash_transactions
    FOR EACH ROW
    EXECUTE FUNCTION cash.update_updated_at_column();
