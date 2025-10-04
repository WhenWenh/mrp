package mrp.domain.ports;

import mrp.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Port für User-Persistenz. Für Zwischenabgabe 1 reichen diese Operationen:
 * - create: neues Konto anlegen (Register)
 * - findByUsername: Login/Existenzcheck
 * - findById: Profile lesen (z.B. /users/me in nächstem Schritt)
 */
public interface UserRepository {

    User create(String username, String passwordHash);

    Optional<User> findByUsername(String username);

    Optional<User> findById(UUID id);

    // Erweiterbar für spätere Meilensteine:
    default void updateProfile(UUID id, String email, String favoriteGenre) {
        throw new UnsupportedOperationException("not implemented");
    }
}
