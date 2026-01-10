package mrp.infrastructure.security;

import com.sun.net.httpserver.HttpExchange;
import mrp.domain.ports.AuthTokenService;

import java.util.UUID;

/**
 * HTTP-level authentication service.
 *
 * Responsibilities:
 * - read the Authorization header from the HTTP request
 * - extract and validate the Bearer token
 * - delegate token verification to AuthTokenService
 *
 * This class belongs to the infrastructure layer and
 * bridges HTTP requests to the domain/application layer.
 */

public class AuthService {

    private AuthTokenService tokens;

    public AuthService(AuthTokenService tokens) {
        this.tokens = tokens;
    }

    // ---- Öffentliche API ----

    /**
     * Reads the Authorization header, verifies the Bearer token
     * and returns an authentication context.
     *
     * @param ex HTTP exchange
     * @return AuthContext containing the authenticated user ID and token
     * @throws IllegalArgumentException if the header is missing or invalid
     * @throws SecurityException if the token is invalid or expired
     */
    public AuthContext requireAuth(HttpExchange ex) {
        if (ex == null) throw new IllegalArgumentException("exchange null");
        String header = ex.getRequestHeaders().getFirst("Authorization");

        // Debug output (can be removed in production)
        System.out.println("DEBUG Auth Header = [" + header + "]");

        String token = extractBearer(header);
        UUID userId = tokens.verifyAndGetUserId(token);
        return new AuthContext(userId, token);
    }

    /**
     * Convenience method that returns only the authenticated user ID.
     *
     * Used by handlers that do not need the full authentication context.
     */
    public UUID requireUserId(HttpExchange ex) {
        return requireAuth(ex).getUserId();
    }

    /*
    public UUID userIdFromAuthHeader(String authHeader) {
        String token = extractBearer(authHeader);
        return tokens.verifyAndGetUserId(token);
    }

    public void revokeFromAuthHeader(String header) {
        if (!(tokens instanceof OpaqueTokenService)) return; // no-op für andere Implementierungen
        String token = extractBearer(header);
        ((OpaqueTokenService) tokens).revoke(token);
    }
    */

    // ---- Helper ----
    /**
     * Extracts the Bearer token from the Authorization header.
     *
     * Expected format:
     * Authorization: Bearer <token>
     *
     * @throws IllegalArgumentException if the header is missing or malformed
     */
    private String extractBearer(String authHeader) {
        if (authHeader == null || authHeader.isBlank())
            throw new IllegalArgumentException("missing Authorization header");
        String p = "Bearer ";
        if (!authHeader.startsWith(p))
            throw new IllegalArgumentException("invalid Authorization header");
        return authHeader.substring(p.length()).trim();
    }

    /**
            * Lightweight authentication context object.
            *
            * Purpose:
            * - bundle userId and token together
     * - avoid exposing token logic outside the security layer
     */
    public class AuthContext {
        private UUID userId;
        private String token;

        public AuthContext(UUID userId, String token) {
            this.userId = userId;
            this.token = token;
        }

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}
