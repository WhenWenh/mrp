package mrp.domain.model;

import java.util.UUID;

public class LeaderboardEntry {
    private UUID userId;
    private String username;
    private int ratingCount;

    public LeaderboardEntry(UUID userId, String username, int ratingCount) {
        if (userId == null) throw new IllegalArgumentException("userId null");
        if (username == null) throw new IllegalArgumentException("username null");
        if (ratingCount < 0) throw new IllegalArgumentException("ratingCount < 0");
        this.userId = userId;
        this.username = username;
        this.ratingCount = ratingCount;
    }

    public UUID getUserId() { return userId; }
    public String getUsername() { return username; }
    public int getRatingCount() { return ratingCount; }
}
