package mrp.application;

import mrp.domain.model.MediaEntry;
import mrp.domain.model.Rating;
import mrp.domain.ports.MediaRepository;
import mrp.domain.ports.RatingRepository;
import mrp.dto.RatingRequest;
import mrp.dto.RatingResponse;
import mrp.infrastructure.util.UUIDv7;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RatingService {

    private RatingRepository ratings;
    private MediaRepository mediaRepo;

    public RatingService(RatingRepository ratings, MediaRepository mediaRepo) {
        if (ratings == null) throw new IllegalArgumentException("ratings null");
        if (mediaRepo == null) throw new IllegalArgumentException("mediaRepo null");
        this.ratings = ratings;
        this.mediaRepo = mediaRepo;
    }

    /**
     * Legt ein neues Rating für ein Media an.
     * - stars 1..5
     * - Kommentar optional
     * - commentConfirmed initial false
     * - likeCount initial 0
     */
    public RatingResponse create(UUID userId, UUID mediaId, RatingRequest req) {
        if (userId == null) throw new IllegalArgumentException("userId null");
        if (mediaId == null) throw new IllegalArgumentException("mediaId null");
        validateRequest(req);

        MediaEntry media = mediaRepo.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("media not found"));

        Rating rating = new Rating(
                UUIDv7.randomUUID(),
                media.getId(),
                userId,
                req.getStars(),
                req.getComment(),
                false,
                null,
                0
        );

        Rating saved = ratings.create(rating);

        // Durchschnitt neu berechnen
        recalcAverageScore(media.getId());

        // Für den Ersteller darf der (noch unbestätigte) Kommentar angezeigt werden
        return toResponse(saved, userId);
    }

    /**
     * Liste aller Ratings zu einem Media.
     * Kommentare:
     * - wenn commentConfirmed=true → sichtbar für alle
     * - wenn false → nur sichtbar für den Autor selbst
     */
    public List<RatingResponse> listForMedia(UUID mediaId, UUID requesterId) {
        if (mediaId == null) throw new IllegalArgumentException("mediaId null");
        if (requesterId == null) throw new IllegalArgumentException("requesterId null");

        // Sicherstellen, dass Media existiert
        mediaRepo.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("media not found"));

        List<Rating> list = ratings.listByMedia(mediaId);
        List<RatingResponse> out = new ArrayList<>();
        for (Rating r : list) {
            out.add(toResponse(r, requesterId));
        }
        return out;
    }

    /**
     * Rating-Historie eines Users (für das eigene Profil).
     * Hier sieht der User immer seinen vollen Kommentar.
     */
    public List<RatingResponse> listForUser(UUID userId) {
        if (userId == null) throw new IllegalArgumentException("userId null");
        List<Rating> list = ratings.listByUser(userId);
        List<RatingResponse> out = new ArrayList<>();
        for (Rating r : list) {
            out.add(toResponse(r, userId));
        }
        return out;
    }

    /**
     * Rating aktualisieren (nur vom Besitzer).
     */
    public void update(UUID ratingId, UUID actorUserId, RatingRequest req) {
        if (ratingId == null) throw new IllegalArgumentException("ratingId null");
        if (actorUserId == null) throw new IllegalArgumentException("actorUserId null");
        validateRequest(req);

        Rating existing = ratings.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException("rating not found"));

        if (!existing.getUserId().equals(actorUserId)) {
            throw new SecurityException("forbidden: not the author of rating");
        }

        // Domain-Objekt aktualisieren
        existing.setStars(req.getStars());
        existing.setComment(req.getComment());
        // Kommentar wurde geändert → erneute Bestätigung nötig
        existing.setCommentConfirmed(false);

        // Persistenz-Schicht updaten (Port ist aktuell noch "flach")
        ratings.update(ratingId, actorUserId, existing.getStars(), existing.getComment());

        // Durchschnitt neu berechnen
        recalcAverageScore(existing.getMediaId());
    }

    /**
     * Rating löschen (nur vom Besitzer).
     */
    public void delete(UUID ratingId, UUID actorUserId) {
        if (ratingId == null) throw new IllegalArgumentException("ratingId null");
        if (actorUserId == null) throw new IllegalArgumentException("actorUserId null");

        Rating existing = ratings.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException("rating not found"));

        if (!existing.getUserId().equals(actorUserId)) {
            throw new SecurityException("forbidden: not the author of rating");
        }

        ratings.delete(ratingId, actorUserId);

        // Durchschnitt neu berechnen
        recalcAverageScore(existing.getMediaId());
    }

    private void validateRequest(RatingRequest req) {
        if (req == null) throw new IllegalArgumentException("request null");
        int stars = req.getStars();
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("stars must be 1..5");
        }
        // Kommentar-Validierung übernimmt Domain (Rating.setComment)
    }

    /**
     * Berechnet den Durchschnittsscore für ein Media neu und speichert ihn.
     */
    private void recalcAverageScore(UUID mediaId) {
        List<Rating> list = ratings.listByMedia(mediaId);
        double avg;
        if (list.isEmpty()) {
            avg = 0.0;
        } else {
            int sum = 0;
            for (Rating r : list) {
                sum += r.getStars();
            }
            avg = sum / (double) list.size();
        }

        MediaEntry media = mediaRepo.findById(mediaId)
                .orElseThrow(() -> new IllegalStateException("media disappeared during rating update"));

        media.setAverageScore(avg);
        mediaRepo.update(media);
    }

    public void confirmComment(UUID ratingId, UUID actorUserId) {
        if (ratingId == null) throw new IllegalArgumentException("ratingId null");
        if (actorUserId == null) throw new IllegalArgumentException("actorUserId null");

        Rating existing = ratings.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException("rating not found"));

        if (!existing.getUserId().equals(actorUserId)) {
            throw new SecurityException("forbidden: not the author of rating");
        }

        String comment = existing.getComment();
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("no comment");
        }

        boolean ok = ratings.confirmComment(ratingId, actorUserId);
        if (!ok) {
            // sollte nach den Checks nicht passieren
            throw new IllegalStateException("confirm failed");
        }
    }


    public void like(UUID ratingId, UUID actorUserId) {
        if (ratingId == null) throw new IllegalArgumentException("ratingId null");
        if (actorUserId == null) throw new IllegalArgumentException("actorUserId null");

        Rating r = ratings.findById(ratingId).orElseThrow(() -> new IllegalArgumentException("rating not found"));
        if(r.getUserId().equals(actorUserId)) {
            throw new SecurityException("forbidden: cannot like own rating");
        }

        boolean ok = ratings.addLike(ratingId, actorUserId);

        if (!ok) throw new IllegalStateException("already liked");

    }

    public void unlike(UUID ratingId, UUID actorUserId) {
        if (ratingId == null) throw new IllegalArgumentException("ratingId null");
        if (actorUserId == null) throw new IllegalArgumentException("actorUserId null");

        Rating r = ratings.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException("rating not found"));

        if (r.getUserId().equals(actorUserId)) {
            throw new SecurityException("forbidden: cannot unlike own rating");
        }

        boolean ok = ratings.removeLike(ratingId, actorUserId);
        if (!ok) throw new IllegalStateException("not liked");
    }


    /**
     * Baut die Response auf Basis Sichtbarkeit des Kommentars.
     */
    private RatingResponse toResponse(Rating r, UUID requesterId) {
        boolean canSeeComment =
                r.isCommentConfirmed() || (requesterId != null && requesterId.equals(r.getUserId()));

        String visibleComment = canSeeComment ? r.getComment() : null;

        return new RatingResponse(
                r.getId(),
                r.getMediaId(),
                r.getUserId(),
                r.getStars(),
                visibleComment,
                r.isCommentConfirmed(),
                r.getCreatedAt(),
                r.getLikeCount()
        );
    }
}
