package mrp.dto;

import mrp.domain.model.enums.MediaType;
import java.util.List;

public class MediaRequest {

    private String title;
    private String description;
    private MediaType mediaType;
    private Integer releaseYear;
    private List<String> genres;
    private Integer ageRestriction;

    public MediaRequest(String title,
                        String description,
                        MediaType mediaType,
                        Integer releaseYear,
                        List<String> genres,
                        Integer ageRestriction) {
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
