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
