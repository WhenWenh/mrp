package mrp.application.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Responsible for secure password hashing and verification.
 * Uses BCrypt.
 */

public class PasswordHasher {

    private int cost;


    // Creates a PasswordHasher with a given BCrypt cost factor.
    public PasswordHasher(int cost) {
        if (cost < 10 || cost > 14) {
            throw new IllegalArgumentException("cost must be between 10 and 14");
        }
        this.cost = cost;
    }

    // Hashes a raw (plain-text) password using BCrypt.
    // A random salt is generated automatically.
    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("raw password null/blank");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(cost));
    }


    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("raw password null");
        }
        if (storedHash == null || storedHash.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(rawPassword, storedHash);
    }
}
