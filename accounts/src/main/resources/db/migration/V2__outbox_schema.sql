-- V2__outbox_schema.sql
-- Outbox table for reliable messaging between accounts and notifications services

CREATE TABLE IF NOT EXISTS accounts.outbox_messages (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
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
