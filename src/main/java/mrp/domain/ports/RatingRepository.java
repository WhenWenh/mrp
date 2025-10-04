package mrp.domain.ports;

import mrp.domain.model.Rating;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimaler Port für Ratings – derzeit nicht benötigt für Register/Login,
 * bleibt aber als Schnittstelle für kommende Features bestehen.
 */
public interface RatingRepository {

    default Rating create(Rating rating) {
        throw new UnsupportedOperationException("not implemented");
    }

    default Optional<Rating> findById(UUID id) {
        return Optional.empty();
    }

    default List<Rating> listByUser(UUID userId) {
        return List.of();
    }

    default void update(UUID ratingId, UUID actorUserId, int stars, String comment) {
        throw new UnsupportedOperationException("not implemented");
    }

    default void delete(UUID ratingId, UUID actorUserId) {
        throw new UnsupportedOperationException("not implemented");
    }
}
