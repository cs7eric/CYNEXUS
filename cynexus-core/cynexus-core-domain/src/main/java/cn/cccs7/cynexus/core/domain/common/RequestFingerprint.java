package cn.cccs7.cynexus.core.domain.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HexFormat;

public final class RequestFingerprint {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RequestFingerprint() {}

    public static String of(Object... values) {
        Object[] canonical = Arrays.stream(values)
                .map(value -> value instanceof LocalDate ? value.toString() : value)
                .toArray();
        try {
            byte[] json = MAPPER.writeValueAsBytes(canonical);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            throw new IllegalStateException("Unable to fingerprint request", e);
        }
    }
}
