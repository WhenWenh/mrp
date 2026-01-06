-- ================================================
--  Extensions
-- ================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- Optional (sehr gut für partial title search)
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
                                        token TEXT NOT NULL, -- Abgabe ok (Prod: nur Hash)
                                        issued_at TIMESTAMPTZ NOT NULL,
                                        expires_at TIMESTAMPTZ NOT NULL,
                                        revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_sessions_user    ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_expires ON sessions(expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS ux_sessions_token ON sessions(token);

-- ================================================
--  Media Entries
-- ================================================
CREATE TABLE IF NOT EXISTS media_entries (
                                             id UUID PRIMARY KEY,

                                             creator_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,

                                             title VARCHAR(255) NOT NULL,
                                             description TEXT,

                                             media_type VARCHAR(16) NOT NULL,
                                             release_year INT,
                                             genres TEXT[],

                                             age_restriction INT NOT NULL DEFAULT 0,
                                             average_score DOUBLE PRECISION NOT NULL DEFAULT 0,

                                             created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                             updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                             CONSTRAINT ck_media_type CHECK (media_type IN ('MOVIE','SERIES','GAME')),
                                             CONSTRAINT ck_age_restriction CHECK (age_restriction >= 0 AND age_restriction <= 21),
                                             CONSTRAINT ck_average_score CHECK (average_score >= 0 AND average_score <= 5),
                                             CONSTRAINT ck_release_year CHECK (release_year IS NULL OR (release_year >= 1800 AND release_year <= 2100))
);

-- Title search:
-- Wenn pg_trgm aktiv ist:
-- CREATE INDEX IF NOT EXISTS idx_media_title_trgm ON media_entries USING gin (title gin_trgm_ops);
-- Fallback:
CREATE INDEX IF NOT EXISTS idx_media_title ON media_entries (title);

-- Filter & sort helper
CREATE INDEX IF NOT EXISTS idx_media_type ON media_entries (media_type);
CREATE INDEX IF NOT EXISTS idx_media_year ON media_entries (release_year);
CREATE INDEX IF NOT EXISTS idx_media_age ON media_entries (age_restriction);
CREATE INDEX IF NOT EXISTS idx_media_avg_score ON media_entries (average_score);

-- Genres: sehr sinnvoll für Filter "genre=..."
CREATE INDEX IF NOT EXISTS idx_media_genres_gin ON media_entries USING gin (genres);

-- ================================================
--  Ratings
-- ================================================
CREATE TABLE IF NOT EXISTS ratings (
                                       id UUID PRIMARY KEY,

                                       media_id UUID NOT NULL REFERENCES media_entries(id) ON DELETE CASCADE,
                                       user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

                                       stars INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
                                       comment TEXT,
                                       comment_confirmed BOOLEAN NOT NULL DEFAULT FALSE,

                                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       like_count INT NOT NULL DEFAULT 0,

                                       CONSTRAINT ux_user_media_unique_rating UNIQUE (media_id, user_id)
);

-- Für "Rating History" / "my ratings"
CREATE INDEX IF NOT EXISTS idx_ratings_user_created ON ratings(user_id, created_at DESC);
-- Für Media-Detailseite + Sort nach Score/Date
CREATE INDEX IF NOT EXISTS idx_ratings_media_created ON ratings(media_id, created_at DESC);

-- ================================================
--  Rating Likes
-- ================================================
CREATE TABLE IF NOT EXISTS rating_likes (
                                            rating_id UUID NOT NULL REFERENCES ratings(id) ON DELETE CASCADE,
                                            user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                            PRIMARY KEY (rating_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_rating_likes_user ON rating_likes(user_id);

-- ================================================
--  Favorites
-- ================================================
CREATE TABLE IF NOT EXISTS favorites (
                                         user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                         media_id UUID NOT NULL REFERENCES media_entries(id) ON DELETE CASCADE,
                                         created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                         PRIMARY KEY (user_id, media_id)
);

CREATE INDEX IF NOT EXISTS idx_favorites_user ON favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_favorites_media ON favorites(media_id);
