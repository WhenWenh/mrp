package mrp.dto;

import java.time.Instant;
import java.util.UUID;

public class RatingResponse {

    private UUID id;
    private UUID mediaId;
    private UUID userId;
    private int stars;
    private String comment;
    private boolean commentConfirmed;
    private Instant createdAt;
    private int likeCount;

    public RatingResponse(
            UUID id,
            UUID mediaId,
            UUID userId,
            int stars,
            String comment,
            boolean commentConfirmed,
            Instant createdAt,
            int likeCount
    ) {
        this.id = id;
        this.mediaId = mediaId;
        this.userId = userId;
        this.stars = stars;
        this.comment = comment;
        this.commentConfirmed = commentConfirmed;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
    }

    public UUID getId() { return id; }
    public UUID getMediaId() { return mediaId; }
    public UUID getUserId() { return userId; }
    public int getStars() { return stars; }
    public String getComment() { return comment; }
    public boolean isCommentConfirmed() { return commentConfirmed; }
    public Instant getCreatedAt() { return createdAt; }
    public int getLikeCount() { return likeCount; }
}
