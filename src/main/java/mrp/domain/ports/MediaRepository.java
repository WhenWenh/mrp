package mrp.domain.ports;

import mrp.domain.model.Media;
import java.util.*;

public interface MediaRepository {
    Media save(Media media);
    Optional<Media> findById(UUID id);
    List<Media> findByType(String mediaType);
    void deleteById(UUID id);
}
