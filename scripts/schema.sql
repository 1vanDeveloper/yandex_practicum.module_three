CREATE DATABASE bank;
\c bank

-- Schema for accounts microservice
CREATE SCHEMA IF NOT EXISTS accounts;

-- Table for storing user accounts
CREATE TABLE IF NOT EXISTS accounts.accounts (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster lookups by login
CREATE INDEX IF NOT EXISTS idx_accounts_login ON accounts.accounts(login);

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION accounts.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_accounts_updated_at
    BEFORE UPDATE ON accounts.accounts
    FOR EACH ROW
    EXECUTE FUNCTION accounts.update_updated_at_column();

-- Outbox table for reliable messaging between accounts and notifications services
CREATE TABLE IF NOT EXISTS accounts.outbox_messages (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    login VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0
);

-- Index for faster lookups by status
CREATE INDEX IF NOT EXISTS idx_outbox_status ON accounts.outbox_messages(status);

-- Index for faster lookups by created_at (for processing order)
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON accounts.outbox_messages(created_at);


-- Schema for notifications microservice
CREATE SCHEMA IF NOT EXISTS notifications;

-- Table for storing notifications
CREATE TABLE IF NOT EXISTS notifications.notifications (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster lookups by login
CREATE INDEX IF NOT EXISTS idx_notifications_login ON notifications.notifications(login);


-- Schema for cash microservice
CREATE SCHEMA IF NOT EXISTS cash;

-- Table for storing cash transactions (deposits and withdrawals)
CREATE TABLE IF NOT EXISTS cash.cash_transactions (
    id BIGSERIAL PRIMARY KEY,
    account_login VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL, -- DEPOSIT or WITHDRAW
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED
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
