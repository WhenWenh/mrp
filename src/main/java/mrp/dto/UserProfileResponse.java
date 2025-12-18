package mrp.dto;

import java.util.UUID;

public class UserProfileResponse {

    public UUID id;
    public String username;
    public String email;
    public String favoriteGenre;

    public int totalRatings;
    public double averageScore;

    public UserProfileResponse() {
    }

    public UserProfileResponse(UUID id,
                               String username,
                               String email,
                               String favoriteGenre,
                               int totalRatings,
                               double averageScore){
        this.id = id;
        this.username = username;
        this.email = email;
        this.favoriteGenre = favoriteGenre;
        this.totalRatings = totalRatings;
        this.averageScore = averageScore;
    }
}
