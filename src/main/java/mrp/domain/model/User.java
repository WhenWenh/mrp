package mrp.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User {

    private UUID id;
    private String username;
    private String passwordHash;
    private String email;
    private String favoriteGenre;
    private Instant createdAt;

    public User() { }

    public User(UUID id,
                String username,
                String passwordHash,
                String email,
                String favoriteGenre,
                Instant createdAt) {
        setId(id);
        setUsername(username);
        setPasswordHash(passwordHash);
        setEmail(email);
        setFavoriteGenre(favoriteGenre);
        setCreatedAt(createdAt);
    }

    public static User newUser(UUID id, String username, String passwordHash) {
        return new User(id, username, passwordHash, null, null, Instant.now());
    }

    public UUID getId() { return id; }
    public void setId(UUID id) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        this.id = id;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("username blank");
        this.username = username.trim();
    }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank())
            throw new IllegalArgumentException("passwordHash blank");
        this.passwordHash = passwordHash;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        if (email == null || email.isBlank()) {
            this.email = null;
        } else {
            this.email = email.trim().toLowerCase();
        }
    }

    public String getFavoriteGenre() { return favoriteGenre; }
    public void setFavoriteGenre(String favoriteGenre) {
        this.favoriteGenre = favoriteGenre == null ? null : favoriteGenre.trim();
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return "User{id=" + id + ", username='" + username + "'}";
    }
}
