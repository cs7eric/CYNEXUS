package cn.cccs7.cynexus.common.security.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Signs the identity assertion exchanged between the trusted gateway and services. */
public final class TrustedPrincipalSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private TrustedPrincipalSigner() {
    }

    public static String sign(String principal, String tenant, String secret) {
        if (isBlank(principal) || isBlank(tenant) || isBlank(secret)) {
            throw new IllegalArgumentException("Trusted identity fields cannot be blank");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(canonical(principal, tenant).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign trusted identity", exception);
        }
    }

    public static boolean verify(String principal, String tenant, String signature, String secret) {
        if (isBlank(signature) || isBlank(secret)) return false;
        try {
            return MessageDigest.isEqual(sign(principal, tenant, secret).getBytes(StandardCharsets.US_ASCII),
                    signature.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String canonical(String principal, String tenant) {
        return principal.trim() + "\n" + tenant.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
