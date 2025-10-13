CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY,
                                     username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    email VARCHAR(255),
    favorite_genre VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS sessions (
                                        jti UUID PRIMARY KEY,
                                        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token TEXT NOT NULL,              -- für Abgabe sichtbar (Prod: nur Hash speichern)
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
    );

CREATE INDEX IF NOT EXISTS idx_sessions_user   ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_expires ON sessions(expires_at);

CREATE TABLE IF NOT EXISTS media_entries (
                                             id UUID PRIMARY KEY,
                                             title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(16) NOT NULL,
    release_year INT,
    age_restriction INT DEFAULT 0,
    creator_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    average_score DOUBLE PRECISION DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS ratings (
                                       id UUID PRIMARY KEY,
                                       media_id UUID REFERENCES media_entries(id) ON DELETE CASCADE,
    user_id  UUID REFERENCES users(id) ON DELETE CASCADE,
    stars INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment TEXT,
    comment_confirmed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    like_count INT NOT NULL DEFAULT 0
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_media_unique_rating
    ON ratings (media_id, user_id);
