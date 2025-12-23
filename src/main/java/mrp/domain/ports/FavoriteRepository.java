package mrp.domain.ports;

import java.util.List;
import java.util.UUID;


public interface FavoriteRepository {
    boolean add(UUID userId, UUID mediaId);

    boolean remove(UUID userId, UUID mediaId);

    List<UUID> listMediaIdsByUser(UUID userId);
}
