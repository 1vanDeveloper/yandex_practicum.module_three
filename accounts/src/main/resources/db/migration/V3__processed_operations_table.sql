-- V3__processed_operations_table.sql
-- Таблица для хранения обработанных operationId (идемпотентность операций)

CREATE TABLE IF NOT EXISTS processed_operations (
    id BIGSERIAL PRIMARY KEY,
    operation_id VARCHAR(255) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    account_login VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    source_service VARCHAR(50) NOT NULL,
    CONSTRAINT uk_operation_id UNIQUE (operation_id)
);

CREATE INDEX IF NOT EXISTS idx_operation_id ON processed_operations(operation_id);
CREATE INDEX IF NOT EXISTS idx_account_login ON processed_operations(account_login);
CREATE INDEX IF NOT EXISTS idx_processed_at ON processed_operations(processed_at);

COMMENT ON TABLE processed_operations IS 'Хранилище обработанных operationId для обеспечения идемпотентности межсервисных операций';
COMMENT ON COLUMN processed_operations.operation_id IS 'Уникальный идентификатор операции в формате: {source}-{type}-{entityId}-{timestamp}';
COMMENT ON COLUMN processed_operations.operation_type IS 'Тип операции: DEBIT, CREDIT, DEPOSIT, WITHDRAW';
COMMENT ON COLUMN processed_operations.account_login IS 'Логин аккаунта, к которому применяется операция';
COMMENT ON COLUMN processed_operations.processed_at IS 'Время обработки операции';
COMMENT ON COLUMN processed_operations.status IS 'Статус обработки: COMPLETED, FAILED';
COMMENT ON COLUMN processed_operations.source_service IS 'Сервис-источник операции: cash, transfer';
