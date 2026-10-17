package cn.cccs7.cynexus.gateway.auth.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Configuration-owned credentials; no account or user database is involved. */
public final class OwnerCredentials {
    private final String hash;
    private final byte[] salt;
    private final byte[] expected;
    private final int iterations;

    public OwnerCredentials(String hash) {
        try {
            String[] parts = hash.split(":");
            if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) {
                throw new IllegalArgumentException("Invalid owner password hash");
            }
            iterations = Integer.parseInt(parts[1]);
            salt = HexFormat.of().parseHex(parts[2]);
            expected = HexFormat.of().parseHex(parts[3]);
            if (iterations < 310000 || iterations > 2000000 || salt.length < 16 || expected.length != 32) {
                throw new IllegalArgumentException("Invalid owner password hash parameters");
            }
            this.hash = hash;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Configure a valid CYNEXUS_OWNER_PASSWORD_HASH before starting gateway");
        }
    }

    public boolean matches(String password) {
        if (password == null || password.length() < 1 || password.length() > 1024) return false;
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            byte[] candidate = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(expected, candidate);
        } catch (Exception exception) {
            throw new IllegalStateException("Password verification unavailable", exception);
        } finally {
            spec.clearPassword();
        }
    }

    public String fingerprint() {
        return digest(hash);
    }

    public static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
