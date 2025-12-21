package mrp.domain.ports;

import mrp.domain.model.Rating;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port für die Persistenz von Ratings.
 *
 * Implementierungen sind verantwortlich für:
 * - Anlegen eines Ratings
 * - Finden eines Ratings
 * - Auflisten von Ratings eines Users bzw. eines Media-Eintrags
 * - Aktualisieren und Löschen mit Berücksichtigung des Besitzer-Users
 */
public interface RatingRepository {

    /**
     * Persistiert ein neues Rating.
     * Erwartet ein Domain-Objekt mit gesetzten IDs (UUIDv7) und Validierung
     * bereits auf Domain-Ebene (stars 1..5 etc.).
     */
    Rating create(Rating rating);

    /**
     * Findet ein Rating anhand seiner ID.
     */
    Optional<Rating> findById(UUID id);

    /**
     * Listet alle Ratings eines bestimmten Users.
     */
    List<Rating> listByUser(UUID userId);

    /**
     * Listet alle Ratings zu einem bestimmten Media-Eintrag.
     * (wird später z.B. für Detailansicht und Score-Berechnung genutzt)
     */
    List<Rating> listByMedia(UUID mediaId);

    /**
     * Aktualisiert ein Rating. Nur der Besitzer (actorUserId == rating.userId)
     * darf eine Änderung vornehmen.
     */
    void update(UUID ratingId, UUID actorUserId, int stars, String comment);

    /**
     * Löscht ein Rating. Nur der Besitzer (actorUserId == rating.userId)
     * darf löschen.
     */
    void delete(UUID ratingId, UUID actorUserId);

    boolean confirmComment(UUID ratingId, UUID actorUserId);

    boolean addLike(UUID ratingId, UUID likerUserId);

    boolean removeLike(UUID ratingId, UUID likerUserId);
}
