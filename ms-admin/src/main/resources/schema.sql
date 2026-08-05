-- Admin Database Schema
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS branding (
    branding_id BIGSERIAL PRIMARY KEY,
    company_id VARCHAR(100) NOT NULL,
    domain VARCHAR(100) UNIQUE,
    logo_url VARCHAR(255),
    favicon_url VARCHAR(255),
    primary_color VARCHAR(7),
    secondary_color VARCHAR(7),
    accent_color VARCHAR(7),
    text_color VARCHAR(7),
    theme VARCHAR(7) DEFAULT 'light',
    login_background_url VARCHAR(255),
    custom_css TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS institutions (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    users_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS institution_metadata (
    institution_id VARCHAR(100) PRIMARY KEY,
    description TEXT,
    website VARCHAR(100),
    contact_email VARCHAR(100),
    phone_number VARCHAR(20),
    address VARCHAR(255),
    subscription_type VARCHAR(20) NOT NULL,
    max_users INT NOT NULL DEFAULT 100,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_institution FOREIGN KEY(institution_id) REFERENCES institutions(id) ON DELETE CASCADE
);