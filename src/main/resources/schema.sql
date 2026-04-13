-- =========================================================
-- TABLE SCHEMA FOR JAVAMATE
-- =========================================================

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Index for faster email lookups
    INDEX idx_users_email (email)
);

-- Note: Since you're using R2DBC (reactive), schema initialization

