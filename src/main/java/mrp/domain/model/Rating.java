package mrp.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Rating {
    private UUID id;
    private UUID mediaId;
    private UUID userId;
    private int stars;                 // 1..5
    private String comment;            // optional
    private boolean commentConfirmed;  // Moderation
    private Instant createdAt;
    private int likeCount;

    public Rating() { }

    public Rating(UUID id, UUID mediaId, UUID userId, int stars, String comment,
                  boolean commentConfirmed, Instant createdAt, int likeCount) {
        this.id = id;
        this.mediaId = mediaId;
        this.userId = userId;
        setStars(stars);
        this.comment = comment;
        this.commentConfirmed = commentConfirmed;
        this.createdAt = createdAt;
        setLikeCount(likeCount);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMediaId() { return mediaId; }
    public void setMediaId(UUID mediaId) { this.mediaId = mediaId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public int getStars() { return stars; }
    public void setStars(int stars) {
        if (stars < 1 || stars > 5)
            throw new IllegalArgumentException("stars must be in [1..5]");
        this.stars = stars;
    }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public boolean isCommentConfirmed() { return commentConfirmed; }
    public void setCommentConfirmed(boolean commentConfirmed) { this.commentConfirmed = commentConfirmed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) {
        if (likeCount < 0) throw new IllegalArgumentException("likeCount >= 0");
        this.likeCount = likeCount;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rating other)) return false;
        return Objects.equals(id, other.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return "Rating{id=%s, mediaId=%s, userId=%s, stars=%d}"
                .formatted(id, mediaId, userId, stars);
    }
}
