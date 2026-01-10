package mrp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import mrp.domain.model.enums.MediaType;
import java.util.List;

public class MediaRequest {
    private String title;
    private String description;
    private MediaType mediaType;
    private Integer releaseYear;
    private List<String> genres;
    private Integer ageRestriction;

    /**
     * Jackson uses this constructor to create a MediaRequest instance from JSON.
     *
     * @JsonCreator tells Jackson that this constructor should be used for deserialization.
     * @JsonProperty maps JSON field names to constructor parameters explicitly.
     *
     * This is required because:
     * - the class has no default (no-args) constructor
     * - the class has no setters
     * - field names in JSON must be bound to constructor arguments
     */

    @JsonCreator
    public MediaRequest(
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("mediaType") MediaType mediaType,
            @JsonProperty("releaseYear") Integer releaseYear,
            @JsonProperty("genres") List<String> genres,
            @JsonProperty("ageRestriction") Integer ageRestriction
    ) {
        this.title = title;
        this.description = description;
        this.mediaType = mediaType;
        this.releaseYear = releaseYear;
        this.genres = genres;
        this.ageRestriction = ageRestriction;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public MediaType getMediaType() { return mediaType; }
    public Integer getReleaseYear() { return releaseYear; }
    public List<String> getGenres() { return genres; }
    public Integer getAgeRestriction() { return ageRestriction; }
}
