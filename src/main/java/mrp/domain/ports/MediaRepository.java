package mrp.domain.ports;

import mrp.domain.model.MediaEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port für die Persistenz von MediaEntry.
 * Die Application-Schicht hängt nur von diesem Interface.
 */
public interface MediaRepository {

    MediaEntry save(MediaEntry entry);

    Optional<MediaEntry> findById(UUID id);

    /**
     * @return true, wenn genau 1 Datensatz aktualisiert wurde.
     */
    boolean update(MediaEntry entry);

    /**
     * @return true, wenn genau 1 Datensatz gelöscht wurde.
     */
    boolean delete(UUID id);

    /**
     * Einfache Such- und Filterfunktion mit Pagination.
     * sortBy: "title" | "year" | "created"
     * sortDir: "asc" | "desc"
     */
    List<MediaEntry> search(MediaSearch search);

    /**
     * Ownership-Check: darf der Nutzer den Datensatz bearbeiten/löschen?
     */
    boolean isOwner(UUID mediaId, UUID userId);
}
