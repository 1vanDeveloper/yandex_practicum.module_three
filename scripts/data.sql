\c bank

-- Sample data for accounts
INSERT INTO accounts.accounts (login, password, email, first_name, last_name, birth_date, amount)
VALUES
    ('john_doe', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS', 'john.doe@example.com', 'John', 'Doe', '1990-05-15', 1000.00),
    ('jane_smith', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS', 'jane.smith@example.com', 'Jane', 'Smith', '1985-08-22', 2500.50)
ON CONFLICT (login) DO NOTHING;
