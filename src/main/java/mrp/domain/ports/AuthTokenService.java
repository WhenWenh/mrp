package mrp.domain.ports;

import java.util.UUID;

/**
 * Port für Token-Handling. Verhindert Abhängigkeit der Application
 * auf eine konkrete Sicherheits-/Token-Implementierung.
 */
public interface AuthTokenService {

    /**
     * Erstellt ein (opaque oder signiertes) Token für den gegebenen User.
     */
    String issueToken(UUID userId, String username);

    /**
     * Validiert ein Token und liefert die User-ID oder wirft eine Exception,
     * wenn das Token ungültig/abgelaufen ist.
     */
    UUID verifyAndGetUserId(String token);
}
