package mrp.domain.ports;

import mrp.domain.model.MediaEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepository {
    MediaEntry save(MediaEntry entry);
    Optional<MediaEntry> findById(UUID id);
    boolean update(MediaEntry entry);
    boolean delete(UUID id);
    List<MediaEntry> search(MediaSearch search);
    boolean isOwner(UUID mediaId, UUID userId);
}
