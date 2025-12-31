package mrp.infrastructure.persistence;

import mrp.domain.model.Rating;
import mrp.domain.ports.RatingRepository;
import mrp.infrastructure.config.ConnectionFactory;
import mrp.infrastructure.util.UUIDv7;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcRatingRepository implements RatingRepository {

    @Override
    public Rating create(Rating rating) {
        if (rating == null) {
            throw new IllegalArgumentException("rating null");
        }

        UUID id = UUIDv7.randomUUID();
        Instant now = Instant.now();

        String sql = """
            INSERT INTO ratings (id, media_id, user_id, stars, comment, comment_confirmed, created_at, like_count)
            VALUES (?,?,?,?,?,?,?,?)
            """;

        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, id);
            ps.setObject(2, rating.getMediaId());
            ps.setObject(3, rating.getUserId());
            ps.setInt(4, rating.getStars());
            ps.setString(5, rating.getComment());
            ps.setBoolean(6, rating.isCommentConfirmed());
            ps.setTimestamp(7, Timestamp.from(now));
            ps.setInt(8, rating.getLikeCount());

            ps.executeUpdate();

            return new Rating(
                    id,
                    rating.getMediaId(),
                    rating.getUserId(),
                    rating.getStars(),
                    rating.getComment(),
                    rating.isCommentConfirmed(),
                    now,
                    rating.getLikeCount()
            );
        } catch (SQLException e) {
            // duplicate key / unique violation
            if (e instanceof org.postgresql.util.PSQLException) {
                org.postgresql.util.PSQLException pe = (org.postgresql.util.PSQLException) e;

                String constraint = pe.getServerErrorMessage() != null
                        ? pe.getServerErrorMessage().getConstraint()
                        : null;

                if ("ux_user_media_unique_rating".equalsIgnoreCase(constraint)) {
                    throw new IllegalStateException("rating already exists");
                }
            }
            throw new RuntimeException("create rating failed", e);
        }
    }

    @Override
    public Optional<Rating> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }

        String sql = """
            SELECT id, media_id, user_id, stars, comment,
                   comment_confirmed, created_at, like_count
            FROM ratings
            WHERE id=?
            """;

        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById rating failed", e);
        }
    }

    @Override
    public List<Rating> listByUser(UUID userId) {
        if (userId == null) {
            return List.of();
        }

        String sql = """
            SELECT id, media_id, user_id, stars, comment,
                   comment_confirmed, created_at, like_count
            FROM ratings
            WHERE user_id=?
            ORDER BY created_at DESC
            """;

        List<Rating> result = new ArrayList<>();

        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("listByUser ratings failed", e);
        }

        return result;
    }

    @Override
    public List<Rating> listByMedia(UUID mediaId) {
        if (mediaId == null) {
            return List.of();
        }

        String sql = """
            SELECT id, media_id, user_id, stars, comment,
                   comment_confirmed, created_at, like_count
            FROM ratings
            WHERE media_id=?
            ORDER BY created_at DESC
            """;

        List<Rating> result = new ArrayList<>();

        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, mediaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("listByMedia ratings failed", e);
        }

        return result;
    }

    @Override
    public void update(UUID ratingId, UUID actorUserId, int stars, String comment) {
        if (ratingId == null || actorUserId == null) {
            throw new IllegalArgumentException("ratingId or actorUserId null");
        }

        String normalizedComment = null;
        if (comment != null) {
            String trimmed = comment.trim();
            if (!trimmed.isEmpty()) {
                normalizedComment = trimmed;
            }
        }

        String sql = """
            UPDATE ratings
            SET stars = ?, comment = ?
            WHERE id = ? AND user_id = ?
            """;

        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, stars);
            ps.setString(2, normalizedComment);
            ps.setObject(3, ratingId);
            ps.setObject(4, actorUserId);

            int n = ps.executeUpdate();
            if (n == 0) {
                // Entweder Rating existiert nicht oder gehört einem anderen User
                throw new IllegalArgumentException("rating not found or forbidden");
            }
        } catch (SQLException e) {
            throw new RuntimeException("update rating failed", e);
        }
    }

    @Override
    public void delete(UUID ratingId, UUID actorUserId) {
        if (ratingId == null || actorUserId == null) {
            throw new IllegalArgumentException("ratingId or actorUserId null");
        }

        String sql = "DELETE FROM ratings WHERE id = ? AND user_id = ?";

        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, ratingId);
            ps.setObject(2, actorUserId);

            int n = ps.executeUpdate();
            if (n == 0) {
                // Entweder Rating existiert nicht oder gehört einem anderen User
                throw new IllegalArgumentException("rating not found or forbidden");
            }
        } catch (SQLException e) {
            throw new RuntimeException("delete rating failed", e);
        }
    }

    private Rating map(ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id");
        UUID mediaId = (UUID) rs.getObject("media_id");
        UUID userId = (UUID) rs.getObject("user_id");
        int stars = rs.getInt("stars");
        String comment = rs.getString("comment");
        boolean commentConfirmed = rs.getBoolean("comment_confirmed");
        Timestamp tsCreated = rs.getTimestamp("created_at");
        Instant createdAt = tsCreated != null ? tsCreated.toInstant() : null;
        int likeCount = rs.getInt("like_count");

        return new Rating(
                id,
                mediaId,
                userId,
                stars,
                comment,
                commentConfirmed,
                createdAt,
                likeCount
        );
    }

    @Override
    public boolean confirmComment(UUID ratingId, UUID actorUserId) {
        String sql = """
                UPDATE ratings
                SET comment_confirmed = true
                WHERE id = ? AND user_id = ? AND COMMENT IS NOT NULL
                """;

        try (Connection c = ConnectionFactory.get()){
            PreparedStatement ps = c.prepareStatement(sql);

            ps.setObject(1, ratingId);
            ps.setObject(2, actorUserId);
            return ps.executeUpdate() == 1;
        }catch (SQLException e){
            throw new RuntimeException("confirmComment failed", e);
        }
    }
    @Override
    public boolean addLike(UUID ratingId, UUID likerUserId) {
        String insert = "INSERT INTO rating_likes (rating_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        String inc = "UPDATE ratings SET like_count = like_count + 1 WHERE id = ?";

        try (Connection c = ConnectionFactory.get()) {
            c.setAutoCommit(false);

            int inserted;
            try (PreparedStatement ps = c.prepareStatement(insert)) {
                ps.setObject(1, ratingId);
                ps.setObject(2, likerUserId);
                inserted = ps.executeUpdate();
            }

            if (inserted != 1) {
                c.rollback();
                return false; // schon geliked
            }

            try (PreparedStatement ps = c.prepareStatement(inc)) {
                ps.setObject(1, ratingId);
                if (ps.executeUpdate() != 1) {
                    c.rollback();
                    return false;
                }
            }

            c.commit();
            return true;


        } catch(SQLException e){
            throw new RuntimeException("addLike failed", e);
        }
    }


    @Override
    public boolean removeLike(UUID ratingId, UUID likerUserId) {
        String del = "DELETE FROM rating_likes WHERE rating_id = ? AND user_id = ?";
        String dec = "UPDATE ratings SET like_count = GREATEST(like_count - 1, 0) WHERE id = ?";

        try (Connection c = ConnectionFactory.get()) {
            c.setAutoCommit(false);

            int removed;
            try (PreparedStatement ps = c.prepareStatement(del)) {
                ps.setObject(1, ratingId);
                ps.setObject(2, likerUserId);
                removed = ps.executeUpdate();
            }

            if (removed != 1) {
                c.rollback();
                return false; // war nicht geliked
            }

            try (PreparedStatement ps = c.prepareStatement(dec)) {
                ps.setObject(1, ratingId);
                if (ps.executeUpdate() != 1) {
                    c.rollback();
                    return false;
                }
            }

            c.commit();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("removeLike failed", e);
        }
    }
}
