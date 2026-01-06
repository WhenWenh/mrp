package mrp.application.security;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {

    private int cost;

    public PasswordHasher(int cost) {
        if (cost < 10 || cost > 14) {
            throw new IllegalArgumentException("cost must be between 10 and 14");
        }
        this.cost = cost;
    }

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
