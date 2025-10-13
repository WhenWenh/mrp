package mrp.infrastructure.util;

import java.security.SecureRandom;
import java.util.UUID;
//TODO zu einer Dependency ändern
/**
 * Einfache UUIDv7-Generierung (RFC 9562) mit Millisekunden-Timestamp + Random.
 * Ergebnis ist eine RFC-4122-konforme UUID (Version=7, Variant=RFC4122).
 */
public class UuidV7 {

    private static final SecureRandom RNG = new SecureRandom();

    public static UUID next() {
        long time = System.currentTimeMillis() & 0xFFFFFFFFFFFFL; // 48 bit
        long randA = RNG.nextLong() & 0x0FFFL;                    // 12 bit

        // MSB: 48b time | 4b version(0x7) | 12b randA
        long msb = (time << 16) | (0x7L << 12) | randA;

        // LSB: set RFC4122 variant '10' in the two MSB bits, keep 62b random
        long randB = RNG.nextLong();
        long lsb = (randB & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new UUID(msb, lsb);
    }

    private UuidV7() { }
}
