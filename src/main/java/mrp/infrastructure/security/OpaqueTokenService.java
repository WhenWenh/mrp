package mrp.infrastructure.security;

import mrp.domain.ports.AuthTokenService;
import mrp.infrastructure.config.ConnectionFactory;
import mrp.infrastructure.util.UUIDv7;

import java.security.SecureRandom;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class OpaqueTokenService implements AuthTokenService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Duration TTL = Duration.ofHours(
            Long.parseLong(env("TOKEN_TTL_HOURS", "3"))
    );

    @Override
    public String issueToken(UUID userId, String username) {
        if (userId == null) throw new IllegalArgumentException("userId null");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username blank");

        String token = username + "-mrp" + newOpaquePart();

        UUID jti = UUIDv7.randomUUID();
        Instant now = Instant.now();
        Instant exp = now.plus(TTL);

        String upsert =
                "INSERT INTO sessions (jti, user_id, token, issued_at, expires_at, revoked) " +
                        "VALUES (?,?,?,?,?,FALSE) " +
                        "ON CONFLICT (user_id) DO UPDATE SET " +
                        "jti = EXCLUDED.jti, " +
                        "token = EXCLUDED.token, " +
                        "issued_at = EXCLUDED.issued_at, " +
                        "expires_at = EXCLUDED.expires_at, " +
                        "revoked = FALSE";

        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(upsert)) {

            ps.setObject(1, jti);
            ps.setObject(2, userId);
            ps.setString(3, token);
            ps.setTimestamp(4, Timestamp.from(now));
            ps.setTimestamp(5, Timestamp.from(exp));
            ps.executeUpdate();

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

                if (revoked) throw new IllegalArgumentException("token revoked");
                if (Instant.now().isAfter(expiresAt))
                    throw new IllegalArgumentException("token expired");

                return (UUID) rs.getObject("user_id");
            }

        } catch (SQLException e) {
            throw new RuntimeException("validate token failed", e);
        }
    }

    /*
    public void revoke(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("token blank");

        String sql = "UPDATE sessions SET revoked=TRUE WHERE token=?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int n = ps.executeUpdate();
            if (n == 0) throw new IllegalArgumentException("session not found");

        } catch (SQLException e) {
            throw new RuntimeException("revoke token failed", e);
        }
    }
    */

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
