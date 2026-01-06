package mrp.application;

import mrp.dto.MediaRequest;
import mrp.dto.MediaResponse;
import mrp.domain.model.MediaEntry;
import mrp.domain.model.enums.MediaType;
import mrp.domain.ports.MediaRepository;
import mrp.domain.ports.MediaSearch;
import mrp.infrastructure.util.UUIDv7;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class MediaService {

    protected MediaRepository repo;

    public MediaService(MediaRepository repo) {
        if (repo == null) throw new IllegalArgumentException("repo null");
        this.repo = repo;
    }

    public MediaResponse create(UUID creatorId, MediaRequest req) {
        if (creatorId == null) throw new IllegalArgumentException("creatorId null");
        validateRequest(req);

        Instant now = Instant.now();
        MediaEntry entry = new MediaEntry(
                UUIDv7.randomUUID(),
                creatorId,
                safeTrim(req.getTitle()),
                req.getDescription(),
                req.getMediaType(),
                req.getReleaseYear(),
                req.getGenres(),
                req.getAgeRestriction(),
                0.0,
                now,
                now
        );

        MediaEntry saved = repo.save(entry);
        return toResponse(saved);
    }

    public MediaResponse get(UUID id) {
        if (id == null) throw new IllegalArgumentException("id null");
        Optional<MediaEntry> opt = repo.findById(id);
        if (opt.isEmpty()) throw new IllegalArgumentException("media not found");
        return toResponse(opt.get());
    }

    public MediaResponse update(UUID id, UUID requesterId, MediaRequest req) {
        if (id == null) throw new IllegalArgumentException("id null");
        if (requesterId == null) throw new IllegalArgumentException("requesterId null");
        validateRequest(req);


        MediaEntry current = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("media not found"));

        if (!current.getCreatorId().equals(requesterId)) throw new SecurityException("forbidden: not the creator");

        current.setTitle(safeTrim(req.getTitle()));
        current.setDescription(req.getDescription());
        current.setMediaType(req.getMediaType());
        current.setReleaseYear(req.getReleaseYear());
        current.setGenres(req.getGenres());
        current.setAgeRestriction(req.getAgeRestriction());
        current.setUpdatedAt(Instant.now());

        if (!repo.update(current)) throw new IllegalStateException("update failed");
        return toResponse(current);
    }

    public void delete(UUID id, UUID requesterId) {
        if (id == null) throw new IllegalArgumentException("id null");
        if (requesterId == null) throw new IllegalArgumentException("requesterId null");

        MediaEntry current = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("media not found"));

        if (!current.getCreatorId().equals(requesterId)) {
            throw new SecurityException("forbidden: not the creator");
        }

        if (!repo.delete(id)) {
            throw new IllegalArgumentException("media not found");
        }
    }


    public List<MediaResponse> search(MediaSearch s) {
        if (s == null) s = new MediaSearch(null, null, null, null, null, null, "title", "asc", 20, 0);
        List<MediaEntry> list = repo.search(s);
        List<MediaResponse> out = new ArrayList<>();
        for (MediaEntry e : list) out.add(toResponse(e));
        return out;
    }

    private void validateRequest(MediaRequest req) {
        if (req == null) throw new IllegalArgumentException("request null");
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) throw new IllegalArgumentException("title blank");
        MediaType mt = req.getMediaType();
        if (mt == null) throw new IllegalArgumentException("mediaType null");
        Integer year = req.getReleaseYear();
        if (year != null && (year < 1888 || year > 2100)) throw new IllegalArgumentException("releaseYear out of range");
        Integer age = req.getAgeRestriction();
        if (age != null && (age < 0 || age > 21)) throw new IllegalArgumentException("ageRestriction out of range");
    }

    private String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private MediaResponse toResponse(MediaEntry e) {
        return new MediaResponse(
                e.getId(), e.getCreatorId(), e.getTitle(), e.getDescription(),
                e.getMediaType(), e.getReleaseYear(), e.getGenres(), e.getAgeRestriction(),
                e.getAverageScore(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
