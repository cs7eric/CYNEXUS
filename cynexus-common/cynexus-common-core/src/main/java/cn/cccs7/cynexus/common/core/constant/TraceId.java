package cn.cccs7.cynexus.common.core.constant;

import java.util.UUID;

public final class TraceId {

    public static final String HEADER_NAME = "X-Trace-Id";
    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TraceId() {
    }

    public static String current() {
        String traceId = HOLDER.get();
        if (traceId == null || traceId.isBlank()) {
            traceId = newTraceId();
            HOLDER.set(traceId);
        }
        return traceId;
    }

    public static void set(String traceId) {
        HOLDER.set(traceId == null || traceId.isBlank() ? newTraceId() : traceId);
    }

    public static void clear() {
        HOLDER.remove();
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
