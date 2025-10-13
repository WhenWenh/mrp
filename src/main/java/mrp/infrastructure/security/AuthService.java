package mrp.infrastructure.security;

import mrp.domain.ports.AuthTokenService;

import java.util.UUID;

public class AuthService {
    private AuthTokenService tokens;
    public AuthService(AuthTokenService tokens) { this.tokens = tokens; }

    public UUID userIdFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            throw new IllegalArgumentException("missing or invalid Authorization header");
        String token = authHeader.substring("Bearer ".length()).trim();
        return tokens.verifyAndGetUserId(token);
    }


    public void revokeFromAuthHeader(String header) {
        if (!(tokens instanceof OpaqueTokenService)) return; // no-op für andere Implementierungen
        if (header == null || header.isBlank()) throw new IllegalArgumentException("missing Authorization header");
        String p = "Bearer ";
        if (!header.startsWith(p)) throw new IllegalArgumentException("invalid Authorization header");
        ((OpaqueTokenService) tokens).revoke(header.substring(p.length()).trim());
    }
}
