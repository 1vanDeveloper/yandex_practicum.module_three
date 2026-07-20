-- Schema for notifications microservice (H2 version for tests)
CREATE SCHEMA IF NOT EXISTS notifications;

-- Table for storing notifications
CREATE TABLE IF NOT EXISTS notifications.notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster lookups by login
CREATE INDEX IF NOT EXISTS idx_notifications_login ON notifications.notifications(login);
