package com.mrp.app.domain;

import java.time.Instant;
import java.util.Objects;

/*
 * Bewertung zu einem Media durch einen User.
 * Kommentar ist erst öffentlich, wenn commentConfirmed == true.
 */
public final class rating {
    private final long id;
    private final long mediaId;
    private final long userId;
    private int stars;                 // 1..5
    private String comment;            // optional
    private boolean commentConfirmed;  // Moderation
    private final Instant createdAt;
    private int likeCount;             // abgeleitet (Likes-Tabelle)

    private rating(Builder b) {
        if (b.id < 0) {
            throw new IllegalArgumentException("id must be >= 0");
        }
        if (b.mediaId <= 0){
            throw new IllegalArgumentException("mediaId must be > 0");
        }
        if (b.userId  <= 0) {
            throw new IllegalArgumentException("userId must be > 0");
        }
        this.id = b.id;
        this.mediaId = b.mediaId;
        this.userId = b.userId;
        setStars(b.stars);
        this.comment = b.comment;
        this.commentConfirmed = b.commentConfirmed;
        this.createdAt = Objects.requireNonNullElseGet(b.createdAt, Instant::now);
        setLikeCount(b.likeCount);
    }

    public long getId() { 
        return id; 
    }
    public long getMediaId() { 
        return mediaId; 
    }
    public long getUserId() { 
        return userId; 
    }
    public int getStars() { 
        return stars;
    }
    public String getComment() { 
        return comment; 
    }
    public boolean isCommentConfirmed() { 
        return commentConfirmed;
    }
    public Instant getCreatedAt() { 
        return createdAt;
    }
    public int getLikeCount() { 
        return likeCount;
    }

    public void setStars(int stars) {
        if (stars < 1 || stars > 5){
            throw new IllegalArgumentException("stars must be in [1..5]");
        }
        this.stars = stars;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public void setCommentConfirmed(boolean confirmed) {
        this.commentConfirmed = confirmed;
    }
    public void setLikeCount(int likeCount) {
        if (likeCount < 0) {
            throw new IllegalArgumentException("likeCount >= 0");
        }
        this.likeCount = likeCount;
    }

    //Builder
    public static class Builder {
        private long id;
        private long mediaId;
        private long userId;
        private int stars = 1;
        private String comment;
        private boolean commentConfirmed;
        private Instant createdAt;
        private int likeCount;

        public Builder id(long id){
            this.id = id;
            return this;
        }
        public Builder mediaId(long mediaId){
            this.mediaId = mediaId;
            return this;
        }
        public Builder userId(long userId){
            this.userId = userId;
            return this;
        }
        public Builder stars(int stars){
            this.stars = stars;
            return this;
        }
        public Builder comment(String comment){
            this.comment = comment;
            return this;
        }
        public Builder commentConfirmed(boolean c){
            this.commentConfirmed = c;
            return this;
        }
        public Builder createdAt(Instant createdAt){
            this.createdAt = createdAt;
            return this;
        }
        public Builder likeCount(int likeCount){
            this.likeCount = likeCount;
            return this;
        }
        public rating build(){
            return new rating(this);
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof rating r)){
            return false;
        }
        return id == r.id;
    }
    @Override public int hashCode() {
        return Long.hashCode(id);
    }

    @Override public String toString() {
        return "Rating{id=%d, mediaId=%d, userId=%d, stars=%d}".formatted(id, mediaId, userId, stars);
    }
}
