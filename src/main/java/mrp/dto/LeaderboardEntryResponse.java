package mrp.dto;

import java.util.UUID;

public class LeaderboardEntryResponse {
    public int rank;
    public UUID userId;
    public String username;
    public int ratingCount;

    public LeaderboardEntryResponse() { }

    public LeaderboardEntryResponse(int rank, UUID userId, String username, int ratingCount) {
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.ratingCount = ratingCount;
    }
}
