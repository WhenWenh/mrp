package mrp.domain.ports;

import mrp.domain.model.Rating;
import java.util.*;

public interface RatingRepository {
    Rating save(Rating rating);
    Optional<Rating> findById(UUID id);
    List<Rating> findByMediaId(String mediaId);
    void deleteById(UUID id);
}
