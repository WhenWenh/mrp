package com.mrp.app.domain;

import java.time.Instant;
import java.util.Objects;

/*
    Benutzer der Plattform. Passwort wird als Hash gespeichert.
 */
public final class user {
    private final long id;
    private final String username;
    private final String passwordHash;
    private String email;                 // veränderbar
    private String favoriteGenre;         // veränderbar
    private final Instant createdAt;

    private user(Builder b) {
        if (b.id < 0){
            throw new IllegalArgumentException("id must be >= 0");
        }
        this.id = b.id;
        this.username = Objects.requireNonNull(b.username, "username");
        if (this.username.isBlank()){
            throw new IllegalArgumentException("username blank");
        }
        this.passwordHash = Objects.requireNonNull(b.passwordHash, "passwordHash");
        this.email = b.email;
        this.favoriteGenre = b.favoriteGenre;
        this.createdAt = Objects.requireNonNullElseGet(b.createdAt, Instant::now);
    }
    //Getter
    public long getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getPasswordHash() {
        return passwordHash;
    }
    public String getEmail() {
        return email;
    }
    public String getFavoriteGenre() {
        return favoriteGenre;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }

    /* Optionaler Update-Punkt für Profilfelder. */
    public void setEmail(String email) {
        this.email = email;
    }
    public void setFavoriteGenre(String favoriteGenre) {
        this.favoriteGenre = favoriteGenre;
    }

    /*Komfort: Statistiken können extern geliefert werden (z. B. aus Repo). */

    // Builder
    public static class Builder {
        private long id;
        private String username;
        private String passwordHash;
        private String email;
        private String favoriteGenre;
        private Instant createdAt;

        public Builder id(long id) {
            this.id = id;
            return this;
        }
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        public Builder favoriteGenre(String favoriteGenre) {
            this.favoriteGenre = favoriteGenre;
            return this;
        }
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public user build() {
            return new user(this);
        }
    }

    // equals/hashCode nach id+username (stabil für Aggregat-Identität)
    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof user u)) {
            return false;
        }
        return id == u.id && Objects.equals(username, u.username);
    }
    @Override public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override public String toString() {
        return "User{id=%d, username='%s'}".formatted(id, username);
    }
}
