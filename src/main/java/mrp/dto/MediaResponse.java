package mrp.dto;

import mrp.domain.model.enums.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MediaResponse {
    private UUID id;
    private UUID creatorId;
    private String title;
    private String description;
    private MediaType mediaType;
    private Integer releaseYear;
    private List<String> genres;
    private Integer ageRestriction;
    private Double averageScore;
    private Instant createdAt;
    private Instant updatedAt;

    public MediaResponse(UUID id,
                         UUID creatorId,
                         String title,
                         String description,
                         MediaType mediaType,
                         Integer releaseYear,
                         List<String> genres,
                         Integer ageRestriction,
                         Double averageScore,
                         Instant createdAt,
                         Instant updatedAt) {

        this.id = id;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.mediaType = mediaType;
        this.releaseYear = releaseYear;
        this.genres = genres;
        this.ageRestriction = ageRestriction;
        this.averageScore = averageScore;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCreatorId() { return creatorId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public MediaType getMediaType() { return mediaType; }
    public Integer getReleaseYear() { return releaseYear; }
    public List<String> getGenres() { return genres; }
    public Integer getAgeRestriction() { return ageRestriction; }
    public Double getAverageScore() { return averageScore; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
