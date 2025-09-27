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

    public User() { } // für Jackson/JDBC

    public User(UUID id, String username, String passwordHash, String email,
                String favoriteGenre, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.favoriteGenre = favoriteGenre;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("username blank");
        this.username = username;
    }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank())
            throw new IllegalArgumentException("passwordHash blank");
        this.passwordHash = passwordHash;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFavoriteGenre() { return favoriteGenre; }
    public void setFavoriteGenre(String favoriteGenre) { this.favoriteGenre = favoriteGenre; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return "User{id=%s, username='%s'}".formatted(id, username);
    }
}
