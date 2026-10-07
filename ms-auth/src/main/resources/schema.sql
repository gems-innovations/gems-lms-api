CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    institution_id VARCHAR(100),
    avatar_url VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);


CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);
CREATE INDEX IF NOT EXISTS idx_users_institution_id ON users(institution_id);

-- Accounts created with a generated (temporary) password must change it on first sign-in.
ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT false;

-- One-time "forgot password" tokens; only their SHA-256 hash is stored.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Immutable audit trail written by the API gateway for authenticated mutations.
CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    institution_id VARCHAR(100),
    action VARCHAR(30) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    resource_path VARCHAR(500) NOT NULL,
    response_status INT NOT NULL,
    client_ip VARCHAR(100),
    user_agent VARCHAR(500),
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_events_institution_time
    ON audit_events(institution_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor_time
    ON audit_events(actor_user_id, occurred_at DESC);

-- E-mail verification: one row per user. Only the SHA-256 hash of the link token is stored.
CREATE TABLE IF NOT EXISTS email_verifications (
    user_id BIGINT PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP
);

-- What e-mail a user wants. No row = defaults (both on). Guests never receive e-mail.
CREATE TABLE IF NOT EXISTS email_preferences (
    user_id BIGINT PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    course_notices BOOLEAN NOT NULL DEFAULT TRUE,
    tips BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
