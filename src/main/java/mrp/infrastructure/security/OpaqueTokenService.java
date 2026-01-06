package mrp.infrastructure.security;

import mrp.domain.ports.AuthTokenService;
import mrp.infrastructure.config.ConnectionFactory;
//import com.github.f4b6a3.uuid.UuidCreator;
import mrp.infrastructure.util.UUIDv7;

import java.security.SecureRandom;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class OpaqueTokenService implements AuthTokenService {


    //TODO: Wenn ein neuer Token generiert wird, vorab überprüfen, wenn ein aktuelles existiert update es
    private static final SecureRandom RNG = new SecureRandom();
    private static final Duration TTL = Duration.ofHours(
            Long.parseLong(env("TOKEN_TTL_HOURS", "24"))
    );

    @Override
    public String issueToken(UUID userId, String username) {
        if (userId == null) throw new IllegalArgumentException("userId null");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username blank");

        String opaquePart = newOpaquePart();
        String token = username + "-mrp" + opaquePart;

        UUID jti = UUIDv7.randomUUID();
        Instant now = Instant.now();
        Instant exp = now.plus(TTL);

        String cleanup = "DELETE FROM sessions WHERE user_id=? AND expires_at < ?";
        String sql = "INSERT INTO sessions (jti, user_id, token, issued_at, expires_at, revoked) VALUES (?,?,?,?,?,FALSE)";

        try (Connection c = ConnectionFactory.get()) {

            // B) abgelaufene Sessions dieses Users löschen
            try (PreparedStatement ps = c.prepareStatement(cleanup)) {
                ps.setObject(1, userId);
                ps.setTimestamp(2, Timestamp.from(now));
                ps.executeUpdate();
            }

            // neue Session anlegen
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setObject(1, jti);
                ps.setObject(2, userId);
                ps.setString(3, token);
                ps.setTimestamp(4, Timestamp.from(now));
                ps.setTimestamp(5, Timestamp.from(exp));
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException("persist session failed", e);
        }

        return token;
    }


    @Override
    public UUID verifyAndGetUserId(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("token blank");

        String sql = "SELECT user_id, revoked, expires_at FROM sessions WHERE token=?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, token);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("invalid token");

                boolean revoked = rs.getBoolean("revoked");
                Instant expiresAt = rs.getTimestamp("expires_at").toInstant();

                //if (revoked) throw new IllegalArgumentException("token revoked");

                if (Instant.now().isAfter(expiresAt)) {
                    // abgelaufene Session entfernen
                    deleteByToken(c, token);
                    throw new IllegalArgumentException("token expired");
                }

                return (UUID) rs.getObject("user_id");
            }

        } catch (SQLException e) {
            throw new RuntimeException("validate token failed", e);
        }
    }

    private void deleteByToken(Connection c, String token) throws SQLException {
        String del = "DELETE FROM sessions WHERE token=?";
        try (PreparedStatement ps = c.prepareStatement(del)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }


    // Optional: Logout-Unterstützung (kann außerhalb des Interfaces bleiben)
    public void revoke(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("token blank");
        String sql = "UPDATE sessions SET revoked=TRUE WHERE token=?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            int n = ps.executeUpdate();
            if (n == 0) throw new IllegalArgumentException("session not found");
        } catch (SQLException e) {
            throw new RuntimeException("revoke token failed", e);
        }
    }

    private static String newOpaquePart() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String env(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
}
