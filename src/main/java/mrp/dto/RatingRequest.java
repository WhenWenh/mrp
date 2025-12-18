package mrp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RatingRequest {

    private int stars;
    private String comment;

    @JsonCreator
    public RatingRequest(
            @JsonProperty("stars") int stars,
            @JsonProperty("comment") String comment
    ) {
        this.stars = stars;
        this.comment = comment;
    }

    public int getStars() { return stars; }
    public String getComment() { return comment; }
}
