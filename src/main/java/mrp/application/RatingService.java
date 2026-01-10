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

/**
 * Application service responsible for managing ratings.
 * Handles creation, updates, deletion, likes, and comment visibility rules.
 */

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
     * Creates a new rating for a media entry.
     *
     * Rules:
     * - stars must be between 1 and 5
     * - comment is optional
     * - commentConfirmed is initially false
     * - likeCount starts at 0
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

        // Recalculate average score of the media
        recalcAverageScore(media.getId());

        // The author can see their own unconfirmed comment
        return toResponse(saved, userId);
    }

    /**
     * Returns all ratings for a media entry.
     *
     * Comment visibility:
     * - confirmed comments are visible to everyone
     * - unconfirmed comments are only visible to the author
     */
    public List<RatingResponse> listForMedia(UUID mediaId, UUID requesterId) {
        if (mediaId == null) throw new IllegalArgumentException("mediaId null");
        if (requesterId == null) throw new IllegalArgumentException("requesterId null");

        // Ensure media exists
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
     * Returns the rating history of a user.
     * The user can always see their full comments.
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
     * Updates an existing rating.
     * Only the author of the rating is allowed to update it.
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

        // Update domain object
        existing.setStars(req.getStars());
        existing.setComment(req.getComment());
        // Comment must be re-confirmed after modification
        existing.setCommentConfirmed(false);

        // Persist changes
        ratings.update(ratingId, actorUserId, existing.getStars(), existing.getComment());

        // Recalculate media average score
        recalcAverageScore(existing.getMediaId());
    }

    /**
     * Deletes a rating.
     * Only the author of the rating may delete it.
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

        // Recalculate media average score
        recalcAverageScore(existing.getMediaId());
    }

    private void validateRequest(RatingRequest req) {
        if (req == null) throw new IllegalArgumentException("request null");
        int stars = req.getStars();
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("stars must be 1..5");
        }
        // Comment validation is handled by the Rating domain object
    }

    /**
     * Recalculates and persists the average score of a media entry.
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

    /**
     * Confirms the comment of a rating.
     * Only the author of the rating may confirm it.
     */
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
            // Should not happen after all checks
            throw new IllegalStateException("confirm failed");
        }
    }

    /**
     * Adds a like to a rating.
     * Users are not allowed to like their own ratings.
     */
    public void like(UUID ratingId, UUID actorUserId) {
        if (ratingId == null) throw new IllegalArgumentException("ratingId null");
        if (actorUserId == null) throw new IllegalArgumentException("actorUserId null");

        Rating r = ratings.findById(ratingId).orElseThrow(() -> new IllegalArgumentException("rating not found"));
        if(r.getUserId().equals(actorUserId)) {
            throw new SecurityException("forbidden: cannot like own rating");
        }

        boolean ok = ratings.addLike(ratingId, actorUserId);

        if (!ok) {
            throw new IllegalStateException("already liked");
        }

    }

    /**
     * Removes a like from a rating.
     */
    public void unlike(UUID ratingId, UUID actorUserId) {
        if (ratingId == null) throw new IllegalArgumentException("ratingId null");
        if (actorUserId == null) throw new IllegalArgumentException("actorUserId null");

        Rating r = ratings.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException("rating not found"));

        if (r.getUserId().equals(actorUserId)) {
            throw new SecurityException("forbidden: cannot unlike own rating");
        }

        boolean ok = ratings.removeLike(ratingId, actorUserId);
        if (!ok) {
            throw new IllegalStateException("not liked");
        }
    }


    /**
     * Builds a RatingResponse based on comment visibility rules.
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
