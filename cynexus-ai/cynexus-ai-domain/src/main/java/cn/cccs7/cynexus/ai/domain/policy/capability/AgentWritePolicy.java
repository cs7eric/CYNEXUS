package cn.cccs7.cynexus.ai.domain.policy.capability;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;

public final class AgentWritePolicy {
    private static final SecureRandom RANDOM = new SecureRandom();
    private AgentWritePolicy() {
    }

    public static String validateAndDigest(String toolCode, String argumentsJson) {
        if (!"knowledge.document.create".equals(toolCode)) throw new IllegalArgumentException("Unsupported Agent write tool");
        if (argumentsJson == null || argumentsJson.isBlank() || argumentsJson.length() > 60_000) {
            throw new IllegalArgumentException("Write arguments are missing or too large");
        }
        return sha256(toolCode + "|" + argumentsJson.trim());
    }

    public static void validateArguments(Map<String, Object> arguments) {
        if (arguments == null) throw new IllegalArgumentException("Write arguments are required");
        String title = text(arguments.get("title"));
        String content = text(arguments.get("content"));
        if (title.length() < 1 || title.length() > 240) throw new IllegalArgumentException("Document title must be 1-240 characters");
        if (content.length() < 1 || content.length() > 50_000) throw new IllegalArgumentException("Document content must be 1-50000 characters");
        String status = text(arguments.get("status"));
        if (!status.isBlank() && !"DRAFT".equalsIgnoreCase(status)) throw new IllegalArgumentException("Agent writes may only create DRAFT documents");
    }

    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }

    public static String issueToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashToken(String token) {
        if (token == null || token.isBlank()) return "";
        return sha256(token.trim());
    }

    public static OffsetDateTime expiresAt() {
        return OffsetDateTime.now().plusMinutes(10);
    }

    private static String sha256(String value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash Agent write command", exception);
        }
    }
}
