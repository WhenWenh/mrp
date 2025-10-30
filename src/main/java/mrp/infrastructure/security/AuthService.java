package mrp.infrastructure.security;

import com.sun.net.httpserver.HttpExchange;
import mrp.domain.ports.AuthTokenService;

import java.util.UUID;

public class AuthService {

    private AuthTokenService tokens;

    public AuthService(AuthTokenService tokens) {
        this.tokens = tokens;
    }

    // ---- Öffentliche API ----

    /** Liest den Authorization-Header, verifiziert das Token und gibt den Context zurück. */
    public AuthContext requireAuth(HttpExchange ex) {
        if (ex == null) throw new IllegalArgumentException("exchange null");
        String header = ex.getRequestHeaders().getFirst("Authorization");

        System.out.println("DEBUG Auth Header = [" + header + "]");

        String token = extractBearer(header);
        UUID userId = tokens.verifyAndGetUserId(token);
        return new AuthContext(userId, token);
    }

    /** Falls du nur die UUID brauchst. */
    public UUID requireUserId(HttpExchange ex) {
        return requireAuth(ex).getUserId();
    }

    /** Weiter nutzbar, wenn du direkt einen Header-String hast. */
    public UUID userIdFromAuthHeader(String authHeader) {
        String token = extractBearer(authHeader);
        return tokens.verifyAndGetUserId(token);
    }

    public void revokeFromAuthHeader(String header) {
        if (!(tokens instanceof OpaqueTokenService)) return; // no-op für andere Implementierungen
        String token = extractBearer(header);
        ((OpaqueTokenService) tokens).revoke(token);
    }

    // ---- Helper ----

    private String extractBearer(String authHeader) {
        if (authHeader == null || authHeader.isBlank())
            throw new IllegalArgumentException("missing Authorization header");
        String p = "Bearer ";
        if (!authHeader.startsWith(p))
            throw new IllegalArgumentException("invalid Authorization header");
        return authHeader.substring(p.length()).trim();
    }

    // Kleiner Context ohne Lombok/record/final
    public static class AuthContext {
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
