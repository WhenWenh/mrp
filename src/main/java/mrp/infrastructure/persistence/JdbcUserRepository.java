package mrp.infrastructure.persistence;

import mrp.domain.model.User;
import mrp.domain.ports.UserRepository;
import mrp.infrastructure.config.ConnectionFactory;
//import com.github.f4b6a3.uuid.UuidCreator;


import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class JdbcUserRepository implements UserRepository {

    @Override
    public User create(String username, String passwordHash) {
        UUID id = UUID.randomUUID();
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
            FROM users WHERE LOWER(username)=LOWER(?)
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
