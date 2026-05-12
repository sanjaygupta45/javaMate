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
    INDEX idx_users_email (email)
    );

-- Create user_documents table
CREATE TABLE IF NOT EXISTS user_documents (
                                              document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              user_id BIGINT NOT NULL,
                                              file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    total_chunks INT DEFAULT 0,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_documents_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_documents_user_id (user_id)
    );

-- Create chat_sessions table
-- One-to-many: users(1) -> chat_sessions(*).
-- This table is the aggregate root for a conversation and is what we use
-- to monitor session activity (status / counts / last-activity timestamps).
CREATE TABLE IF NOT EXISTS chat_sessions (
                                             session_id VARCHAR(36) NOT NULL PRIMARY KEY,
                                             user_id BIGINT NOT NULL,
                                             title VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    message_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_chat_sessions_user (user_id, last_message_at),
    INDEX idx_chat_sessions_status (user_id, status)
    );

-- Create chat_messages table
-- One-to-many: chat_sessions(1) -> chat_messages(*).
CREATE TABLE IF NOT EXISTS chat_messages (
                                             message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             user_id BIGINT NOT NULL,
                                             session_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    INDEX idx_chat_session_lookup (user_id, session_id, created_at)
    );

-- Verify tables
SHOW TABLES;

-- Exit MySQL
EXIT;