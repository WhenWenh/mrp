package mrp.infrastructure.persistence;

import mrp.domain.model.User;
import mrp.domain.ports.UserRepository;
import mrp.infrastructure.config.ConnectionFactory;
import mrp.infrastructure.util.UUIDv7;
import mrp.dto.LeaderboardEntry;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class JdbcUserRepository implements UserRepository {

    @Override
    public User create(String username, String passwordHash) {
        UUID id = UUIDv7.randomUUID();
        Instant now = Instant.now();
        String sql = "INSERT INTO users (id, username, password_hash, created_at) VALUES (?,?,?,?)";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, username);
            ps.setString(3, passwordHash);
            ps.setTimestamp(4, Timestamp.from(now));
            ps.executeUpdate();
            return new User(id, username, passwordHash, null, null, now);
        } catch (SQLException e) {
            // simple unique check
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate"))
                throw new IllegalStateException("username already exists");
            throw new RuntimeException("create user failed", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = """
        SELECT id, username, password_hash, email, favorite_genre, created_at
        FROM users
        WHERE username = ?
    """;

        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("findByUsername failed", e);
        }
    }

    @Override
    public Optional<User> findById(UUID id) {
        String sql = """
            SELECT id, username, password_hash, email, favorite_genre, created_at
            FROM users WHERE id=?
        """;
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
    }

    @Override
    public void updateProfile(UUID id, String email, String favoriteGenre) {
        String sql = "UPDATE users SET email=?, favorite_genre=? WHERE id=?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, favoriteGenre);
            ps.setObject(3, id);
            int n = ps.executeUpdate();
            if (n == 0) throw new IllegalArgumentException("user not found");
        } catch (SQLException e) {
            throw new RuntimeException("updateProfile failed", e);
        }
    }

    @Override
    public List<LeaderboardEntry> leaderboardByRatings(int limit, int offset) {
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        int safeOffset = Math.max(offset, 0);

        String sql = """
        SELECT u.id, u.username, COUNT(r.id) AS rating_count
        FROM users u
        LEFT JOIN ratings r ON r.user_id = u.id
        GROUP BY u.id, u.username
        ORDER BY rating_count DESC, u.username ASC
        LIMIT ? OFFSET ?
        """;

        List<LeaderboardEntry> result = new ArrayList<>();

        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, safeLimit);
            ps.setInt(2, safeOffset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID id = (UUID) rs.getObject("id");
                    String username = rs.getString("username");
                    int count = rs.getInt("rating_count");
                    result.add(new LeaderboardEntry(id, username, count));
                }
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("leaderboardByRatings failed", e);
        }
    }


    private User map(ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String email = rs.getString("email");
        String favorite = rs.getString("favorite_genre");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        return new User(id, username, passwordHash, email, favorite, createdAt);
    }
}
