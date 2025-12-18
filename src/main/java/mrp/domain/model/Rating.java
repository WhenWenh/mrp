package mrp.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Rating {

    private UUID id;
    private UUID mediaId;
    private UUID userId;
    private int stars;
    private String comment;
    private boolean commentConfirmed;
    private Instant createdAt;
    private int likeCount;

    public Rating() { }

    public Rating(UUID id,
                  UUID mediaId,
                  UUID userId,
                  int stars,
                  String comment,
                  boolean commentConfirmed,
                  Instant createdAt,
                  int likeCount) {
        setId(id);
        setMediaId(mediaId);
        setUserId(userId);
        setStars(stars);
        setComment(comment);
        setCommentConfirmed(commentConfirmed);
        setCreatedAt(createdAt);
        setLikeCount(likeCount);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id null");
        }
        this.id = id;
    }

    public UUID getMediaId() { return mediaId; }
    public void setMediaId(UUID mediaId) {
        if (mediaId == null) {
            throw new IllegalArgumentException("mediaId null");
        }
        this.mediaId = mediaId;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId null");
        }
        this.userId = userId;
    }

    public int getStars() { return stars; }
    public void setStars(int stars) {
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("stars must be 1..5");
        this.stars = stars;
    }

    public String getComment() { return comment; }
    public void setComment(String comment) {
        this.comment = (comment == null || comment.isBlank()) ? null : comment.trim();
    }

    public boolean isCommentConfirmed() { return commentConfirmed; }
    public void setCommentConfirmed(boolean commentConfirmed) { this.commentConfirmed = commentConfirmed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) {
        this.likeCount = Math.max(likeCount, 0);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rating other)) return false;
        return Objects.equals(id, other.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return "Rating{id=" + id + ", stars=" + stars + "}";
    }
}
