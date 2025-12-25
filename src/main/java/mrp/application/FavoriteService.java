package mrp.application;

import mrp.dto.MediaResponse;
import mrp.domain.model.MediaEntry;
import mrp.domain.ports.FavoriteRepository;
import mrp.domain.ports.MediaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FavoriteService {
    private FavoriteRepository favorites;
    private MediaRepository media;

    public FavoriteService(FavoriteRepository favorites, MediaRepository media) {
        if (favorites == null) throw new IllegalArgumentException("favorites null");
        if (media == null) throw new IllegalArgumentException("media null");
        this.favorites = favorites;
        this.media = media;
    }

    public void add(UUID userId, UUID mediaId) {
        if (userId == null) throw new IllegalArgumentException("userId null");
        if (mediaId == null) throw new IllegalArgumentException("mediaId null");

        // 404 sauber abbildbar: Media muss existieren
        media.findById(mediaId).orElseThrow(() -> new IllegalArgumentException("media not found"));

        favorites.add(userId, mediaId); // wenn schon da: OK (idempotent)
    }

    public void remove(UUID userId, UUID mediaId) {
        if (userId == null) throw new IllegalArgumentException("userId null");
        if (mediaId == null) throw new IllegalArgumentException("mediaId null");

        favorites.remove(userId, mediaId); // wenn nicht da: OK (idempotent)
    }

    public List<MediaResponse> listMine(UUID userId) {
        if (userId == null) throw new IllegalArgumentException("userId null");

        List<UUID> ids = favorites.listMediaIdsByUser(userId);
        List<MediaResponse> out = new ArrayList<>();

        for (UUID id : ids) {
            MediaEntry e = media.findById(id).orElse(null);
            if (e != null) {
                out.add(new MediaResponse(
                        e.getId(), e.getCreatorId(), e.getTitle(), e.getDescription(),
                        e.getMediaType(), e.getReleaseYear(), e.getGenres(), e.getAgeRestriction(),
                        e.getAverageScore(), e.getCreatedAt(), e.getUpdatedAt()
                ));
            }
        }
        return out;
    }
}
