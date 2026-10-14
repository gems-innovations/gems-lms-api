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
    type VARCHAR(20),
    logo_url VARCHAR(255),
    icon_url VARCHAR(255),
    favicon_url VARCHAR(255),
    primary_color VARCHAR(7),
    secondary_color VARCHAR(7),
    accent_color VARCHAR(7),
    text_color VARCHAR(7),
    background_color VARCHAR(7),
    dark_mode BOOLEAN DEFAULT false,
    theme VARCHAR(7) DEFAULT 'light',
    login_background_url VARCHAR(255),
    custom_css TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_branding_company_id ON branding(company_id);

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

-- Home personalizada por institución (HTML + CSS, sin JavaScript). El slug es el subdominio.
CREATE TABLE IF NOT EXISTS institution_home (
    institution_id VARCHAR(100) PRIMARY KEY,
    slug VARCHAR(40) UNIQUE,
    draft_html TEXT,
    draft_css TEXT,
    draft_updated_at TIMESTAMP,
    draft_updated_by BIGINT,
    published_version INT,
    CONSTRAINT fk_home_institution FOREIGN KEY(institution_id) REFERENCES institutions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS institution_home_versions (
    id BIGSERIAL PRIMARY KEY,
    institution_id VARCHAR(100) NOT NULL,
    version INT NOT NULL,
    html TEXT,
    css TEXT,
    note VARCHAR(200),
    published_by BIGINT,
    published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_home_version_institution FOREIGN KEY(institution_id) REFERENCES institutions(id) ON DELETE CASCADE,
    CONSTRAINT uq_home_version UNIQUE (institution_id, version)
);

-- Modo de la home: BASE (landing de GEMS con la marca y el CSS de la institución) o CUSTOM (HTML propio).
ALTER TABLE institution_home ADD COLUMN IF NOT EXISTS draft_mode VARCHAR(10) NOT NULL DEFAULT 'BASE';
ALTER TABLE institution_home_versions ADD COLUMN IF NOT EXISTS mode VARCHAR(10) NOT NULL DEFAULT 'BASE';
