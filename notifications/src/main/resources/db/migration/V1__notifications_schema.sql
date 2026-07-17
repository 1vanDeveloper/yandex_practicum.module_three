-- V1__notifications_schema.sql
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
