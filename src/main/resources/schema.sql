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

-- Create user_documents table to store uploaded document metadata
CREATE TABLE IF NOT EXISTS user_documents (
    document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    total_chunks INT DEFAULT 0,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign key to users table
    CONSTRAINT fk_user_documents_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,

    -- Index for faster user document lookups
    INDEX idx_user_documents_user_id (user_id)
);


