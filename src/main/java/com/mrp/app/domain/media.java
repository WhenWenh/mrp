package com.mrp.app.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/*
 * Medieneintrag (Film/Serie/Game) mit Aggregatszustand (Durchschnittsrating).
 * Hinweis: averageScore wird i. d. R. in der DB berechnet/aktualisiert.
 */
public final class media {
    private final long id;
    private String title;                 // veränderbar (Owner-check außerhalb)
    private String description;           // veränderbar
    private mediaType type;               // veränderbar
    private int releaseYear;              // veränderbar
    private List<String> genres;          // veränderbar
    private int ageRestriction;           // veränderbar
    private final long creatorUserId;
    private double averageScore;          // von Ratings abgeleitet (0..5)

    private media(Builder b) {
        if (b.id < 0){
            throw new IllegalArgumentException("id must be >= 0");
        }

        this.id = b.id;
        setTitle(Objects.requireNonNull(b.title, "title"));
        setDescription(Objects.requireNonNull(b.description, "description"));
        setType(Objects.requireNonNull(b.type, "type"));
        setReleaseYear(b.releaseYear);
        setGenres(b.genres != null ? b.genres : List.of());
        setAgeRestriction(b.ageRestriction);

        if (b.creatorUserId <= 0){
            throw new IllegalArgumentException("creatorUserId must be > 0");
        }

        this.creatorUserId = b.creatorUserId;
        setAverageScore(b.averageScore);
    }

    // == Getter ==
    public long getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getDescription() {
        return description;
    }
    public mediaType getType() {
        return type;
    }
    public int getReleaseYear() {
        return releaseYear;
    }
    public List<String> getGenres() {
        return Collections.unmodifiableList(genres);
    }
    public int getAgeRestriction() {
        return ageRestriction;
    }
    public long getCreatorUserId() {
        return creatorUserId;
    }
    public double getAverageScore() {
        return averageScore;
    }


    //Mutations mit Validation (für Update-Use-Cases)
    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title blank");
        }
        this.title = title;
    }
    public void setDescription(String description) {
        if (description == null || description.isBlank()){
            throw new IllegalArgumentException("description blank");
        }
        this.description = description;
    }
    public void setType(mediaType type) {
        this.type = Objects.requireNonNull(type, "type");
    }
    public void setReleaseYear(int year) {
        if (year < 1870 || year > 2025){
            throw new IllegalArgumentException("releaseYear out of range");
        }
        this.releaseYear = year;
    }
    public void setGenres(List<String> genres) {
        var copy = new ArrayList<String>(Objects.requireNonNull(genres, "genres"));
        copy.replaceAll(g -> Objects.requireNonNull(g, "genre").trim());
        this.genres = copy;
    }
    public void setAgeRestriction(int ar) {
        if (ar < 0 || ar > 18){
            throw new IllegalArgumentException("ageRestriction out of range");
        }
        this.ageRestriction = ar;
    }
    public void setAverageScore(double avg) {
        if (avg < 0.0 || avg > 5.0) {
            throw new IllegalArgumentException("averageScore must be [0..5]");
        }
        this.averageScore = avg;
    }

    // == Builder ==
    public static class Builder {
        private long id;
        private String title;
        private String description;
        private mediaType type;
        private int releaseYear;
        private List<String> genres = new ArrayList<>();
        private int ageRestriction;
        private long creatorUserId;
        private double averageScore;

        public Builder id(long id){
            this.id = id;
            return this;
        }
        public Builder title(String title){
            this.title = title;
            return this;
        }
        public Builder description(String description){
            this.description = description;
            return this;
        }
        public Builder type(mediaType type){
            this.type = type;
            return this;
        }
        public Builder releaseYear(int year){
            this.releaseYear = year;
            return this;
        }
        public Builder genres(List<String> genres){
            this.genres = genres;
            return this;
        }
        public Builder ageRestriction(int ageRestriction){
            this.ageRestriction = ageRestriction;
            return this;
        }
        public Builder creatorUserId(long creatorUserId){
            this.creatorUserId = creatorUserId;
            return this;
        }
        public Builder averageScore(double averageScore){
            this.averageScore = averageScore;
            return this;
        }

        public media build(){
            return new media(this);
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof media m)){
            return false;
        }
        return id == m.id;
    }
    @Override public int hashCode() {
        return Long.hashCode(id);
    }

    @Override public String toString() {
        return "Media{id=%d, title='%s', type=%s}".formatted(id, title, type);
    }
}
