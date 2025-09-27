package com.mrp.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Media {
    private UUID id;
    private String title;
    private String description;
    private MediaType type;
    private int releaseYear;
    private List<String> genres = new ArrayList<>();
    private int ageRestriction;
    private UUID creatorUserId;
    private double averageScore; // 0..5, aus Ratings abgeleitet

    public Media() { }

    public Media(UUID id, String title, String description, MediaType type,
                 int releaseYear, List<String> genres, int ageRestriction,
                 UUID creatorUserId, double averageScore) {
        this.id = id;
        setTitle(title);
        setDescription(description);
        setType(type);
        setReleaseYear(releaseYear);
        setGenres(genres);
        setAgeRestriction(ageRestriction);
        this.creatorUserId = creatorUserId;
        setAverageScore(averageScore);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("title blank");
        this.title = title;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("description blank");
        this.description = description;
    }

    public MediaType getType() { return type; }
    public void setType(MediaType type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public int getReleaseYear() { return releaseYear; }
    public void setReleaseYear(int releaseYear) {
        if (releaseYear < 1870 || releaseYear > 2100)
            throw new IllegalArgumentException("releaseYear out of range");
        this.releaseYear = releaseYear;
    }

    public List<String> getGenres() {
        return Collections.unmodifiableList(genres);
    }
    public void setGenres(List<String> genres) {
        if (genres == null) { this.genres = new ArrayList<>(); return; }
        ArrayList<String> copy = new ArrayList<>(genres.size());
        for (String g : genres) {
            if (g == null) continue;
            String t = g.trim();
            if (!t.isEmpty()) copy.add(t);
        }
        this.genres = copy;
    }

    public int getAgeRestriction() { return ageRestriction; }
    public void setAgeRestriction(int ageRestriction) {
        if (ageRestriction < 0 || ageRestriction > 21)
            throw new IllegalArgumentException("ageRestriction out of range");
        this.ageRestriction = ageRestriction;
    }

    public UUID getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(UUID creatorUserId) { this.creatorUserId = creatorUserId; }

    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) {
        if (averageScore < 0.0 || averageScore > 5.0)
            throw new IllegalArgumentException("averageScore must be [0..5]");
        this.averageScore = averageScore;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Media other)) return false;
        return Objects.equals(id, other.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return "Media{id=%s, title='%s', type=%s}".formatted(id, title, type);
    }
}
