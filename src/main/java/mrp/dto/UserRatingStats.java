package mrp.dto;

public class UserRatingStats {
    public int totalRatings;
    public double averageScore;

    public UserRatingStats() { }

    public UserRatingStats(int totalRatings, double averageScore) {
        this.totalRatings = totalRatings;
        this.averageScore = averageScore;
    }
}
