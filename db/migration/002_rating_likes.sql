-- ================================================
--  Rating Likes (1 Like pro User pro Rating)
-- ================================================
CREATE TABLE IF NOT EXISTS rating_likes (
                                            rating_id UUID NOT NULL REFERENCES ratings(id) ON DELETE CASCADE,
    user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (rating_id, user_id)
    );

CREATE INDEX IF NOT EXISTS idx_rating_likes_user ON rating_likes(user_id);
