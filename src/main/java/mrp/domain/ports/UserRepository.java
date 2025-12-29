package mrp.domain.ports;

import mrp.domain.model.User;
import mrp.domain.model.LeaderboardEntry;

import java.util.List;
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

    void updateProfile(UUID id, String email, String favoriteGenre);

    default List<LeaderboardEntry> leaderboardByRatings(int limit, int offset) {
        throw new UnsupportedOperationException("not implemented");
    }
}
