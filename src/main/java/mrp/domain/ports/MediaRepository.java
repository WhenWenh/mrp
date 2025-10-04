package mrp.domain.ports;

import mrp.domain.model.MediaEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimaler Port für Media – für Zwischenabgabe 1 noch nicht genutzt,
 * aber als Vertragsanker für SOLID/Layering bereits vorhanden.
 */
public interface MediaRepository {

    default MediaEntry create(MediaEntry entry) {
        throw new UnsupportedOperationException("not implemented");
    }

    default Optional<MediaEntry> findById(UUID id) {
        return Optional.empty();
    }

    default List<MediaEntry> search(String query) {
        return List.of();
    }

    default void update(MediaEntry entry, UUID actorUserId) {
        throw new UnsupportedOperationException("not implemented");
    }

    default void delete(UUID id, UUID actorUserId) {
        throw new UnsupportedOperationException("not implemented");
    }
}
