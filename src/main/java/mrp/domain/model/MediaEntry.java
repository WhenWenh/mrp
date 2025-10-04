package mrp.domain.model;

import mrp.domain.model.enums.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MediaEntry {

    private UUID id;
    private String title;
    private String description;
    private MediaType type;
    private int releaseYear;
    private List<String> genres = new ArrayList<>();
    private int ageRestriction;
    private UUID creatorUserId;
    private double averageScore; // 0..5 (wird aus Ratings berechnet)

    public MediaEntry() { }

    public MediaEntry(UUID id,
                      String title,
                      String description,
                      MediaType type,
                      int releaseYear,
                      List<String> genres,
                      int ageRestriction,
                      UUID creatorUserId,
                      double averageScore) {
        setId(id);
        setTitle(title);
        setDescription(description);
        setType(type);
        setReleaseYear(releaseYear);
        setGenres(genres);
        setAgeRestriction(ageRestriction);
        setCreatorUserId(creatorUserId);
        setAverageScore(averageScore);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        this.id = id;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("title blank");
        this.title = title.trim();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    public MediaType getType() { return type; }
    public void setType(MediaType type) {
        if (type == null) throw new IllegalArgumentException("type null");
        this.type = type;
    }

    public int getReleaseYear() { return releaseYear; }
    public void setReleaseYear(int releaseYear) {
        this.releaseYear = releaseYear;
    }

    public List<String> getGenres() {
        return Collections.unmodifiableList(genres);
    }
    public void setGenres(List<String> genres) {
        this.genres.clear();
        if (genres != null) {
            for (String g : genres) {
                if (g != null && !g.isBlank()) this.genres.add(g.trim());
            }
        }
    }

    public int getAgeRestriction() { return ageRestriction; }
    public void setAgeRestriction(int ageRestriction) {
        this.ageRestriction = Math.max(ageRestriction, 0);
    }

    public UUID getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(UUID creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) {
        // Akzeptiere 0..5 (weich, Service setzt korrekt)
        if (averageScore < 0 || averageScore > 5) averageScore = 0.0;
        this.averageScore = averageScore;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MediaEntry other)) return false;
        return Objects.equals(id, other.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return "MediaEntry{id=" + id + ", title='" + title + "'}";
    }
}
