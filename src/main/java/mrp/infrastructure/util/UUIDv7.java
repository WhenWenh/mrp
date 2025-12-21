/*package mrp.infrastructure.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Einfache UUIDv7-Generierung (RFC 9562) mit Millisekunden-Timestamp + Random.
 * Ergebnis ist eine RFC-4122-konforme UUID (Version=7, Variant=RFC4122).

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
*/

package mrp.infrastructure.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.UUID;

public class UUIDv7 {

    // bewusst nicht 'final', gemäß Projektvorgabe
    private static SecureRandom random = new SecureRandom();

    public static UUID randomUUID() {
        byte[] value = randomBytes();
        ByteBuffer buf = ByteBuffer.wrap(value);
        long high = buf.getLong();
        long low = buf.getLong();
        return new UUID(high, low);
    }

    public static byte[] randomBytes() {
        // random bytes
        byte[] value = new byte[16];
        random.nextBytes(value);

        // current timestamp in ms
        ByteBuffer timestamp = ByteBuffer.allocate(Long.BYTES);
        timestamp.putLong(System.currentTimeMillis());

        // timestamp (untere 48 Bit, big-endian in value[0..5])
        System.arraycopy(timestamp.array(), 2, value, 0, 6);

        // version and variant
        value[6] = (byte) ((value[6] & 0x0F) | 0x70);
        value[8] = (byte) ((value[8] & 0x3F) | 0x80);

        return value;
    }


}

