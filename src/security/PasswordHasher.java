package security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {
    private static final SecureRandom RNG = new SecureRandom();
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private PasswordHasher() {}

    public static String hash(String passwordPlain) {
        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);
        byte[] dk = pbkdf2(passwordPlain.toCharArray(), salt, ITERATIONS, KEY_BITS);
        return "pbkdf2$" + ITERATIONS + "$" + b64(salt) + "$" + b64(dk);
    }

    public static boolean verify(String passwordPlain, String stored) {
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 4) return false;
            int it = Integer.parseInt(parts[1]);
            byte[] salt = b64d(parts[2]);
            byte[] expected = b64d(parts[3]);
            byte[] actual = pbkdf2(passwordPlain.toCharArray(), salt, it, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Hashing error: " + e.getMessage(), e);
        }
    }

    private static String b64(byte[] x) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(x);
    }
    private static byte[] b64d(String s) {
        return Base64.getUrlDecoder().decode(s);
    }
}
