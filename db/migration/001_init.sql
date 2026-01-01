-- ================================================
--  Extensions (einmal pro Datenbank)
-- ================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- Optional: nur wenn du Trigram willst (ansonsten wird Fallback-Index genutzt)
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ================================================
--  Users
-- ================================================
CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY,
                                     username VARCHAR(64) NOT NULL UNIQUE,
                                     password_hash VARCHAR(256) NOT NULL,
                                     email VARCHAR(255),
                                     favorite_genre VARCHAR(64),
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ================================================
--  Sessions
-- ================================================
CREATE TABLE IF NOT EXISTS sessions (
                                        jti UUID PRIMARY KEY,
                                        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                        token TEXT NOT NULL,                  -- für Abgabe sichtbar (Prod: nur Hash speichern)
                                        issued_at TIMESTAMPTZ NOT NULL,
                                        expires_at TIMESTAMPTZ NOT NULL,
                                        revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_sessions_user   ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_expires ON sessions(expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS ux_sessions_token ON sessions(token);

-- ================================================
--  Media Entries (einheitlich)
-- ================================================
CREATE TABLE IF NOT EXISTS media_entries (
                                             id UUID PRIMARY KEY,
                                             creator_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                             title TEXT NOT NULL,
                                             description TEXT,
                                             media_type TEXT NOT NULL,             -- "MOVIE" | "SERIES" | "GAME"
                                             release_year INT,
                                             genres TEXT[],                        -- optional: statt ARRAY auch CSV
                                             age_restriction INT,
                                             average_score DOUBLE PRECISION DEFAULT 0,
                                             created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                             updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ================================================
--  Indizes (Fallback ohne pg_trgm)
-- ================================================
-- Wenn pg_trgm installiert ist, kannst du stattdessen:
-- CREATE INDEX IF NOT EXISTS idx_media_title_trgm ON media_entries USING gin (title gin_trgm_ops);
-- verwenden. Fallback:
CREATE INDEX IF NOT EXISTS idx_media_title ON media_entries (title);

CREATE INDEX IF NOT EXISTS idx_media_type ON media_entries (media_type);
CREATE INDEX IF NOT EXISTS idx_media_year ON media_entries (release_year);

-- ================================================
--  Ratings
-- ================================================
CREATE TABLE IF NOT EXISTS ratings (
                                       id UUID PRIMARY KEY,
                                       media_id UUID REFERENCES media_entries(id) ON DELETE CASCADE,
                                       user_id  UUID REFERENCES users(id) ON DELETE CASCADE,
                                       stars INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
                                       comment TEXT,
                                       comment_confirmed BOOLEAN DEFAULT FALSE,
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       like_count INT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_media_unique_rating
    ON ratings (media_id, user_id);

CREATE TABLE IF NOT EXISTS rating_likes (
                                            rating_id UUID NOT NULL REFERENCES ratings(id) ON DELETE CASCADE,
                                            user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                            PRIMARY KEY (rating_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_rating_likes_user ON rating_likes(user_id);

-- Favorites: User kann Media als Favorit markieren (1x pro User/Media)
CREATE TABLE IF NOT EXISTS favorites (
                                         user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                         media_id UUID NOT NULL REFERENCES media_entries(id) ON DELETE CASCADE,
                                         created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                         PRIMARY KEY (user_id, media_id)
);

CREATE INDEX IF NOT EXISTS idx_favorites_user ON favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_favorites_media ON favorites(media_id);