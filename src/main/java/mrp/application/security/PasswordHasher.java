package mrp.application.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Einfache SHA-256 Hashing-Hilfe für Passwörter.
 * (Für die Zwischenabgabe ausreichend; später kann Argon2/BCrypt folgen.)
 */
public class PasswordHasher {

    public static String sha256(String raw) {
        if (raw == null) throw new IllegalArgumentException("raw password null");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b: bytes) {
            String hx = Integer.toHexString((b & 0xff) | 0x100).substring(1);
            sb.append(hx);
        }
        return sb.toString();
    }
}
