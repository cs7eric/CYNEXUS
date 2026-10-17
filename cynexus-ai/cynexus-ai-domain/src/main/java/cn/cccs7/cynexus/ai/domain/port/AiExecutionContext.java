package cn.cccs7.cynexus.ai.domain.port;

/** Correlates one model invocation across routing, logs and observability. */
public record AiExecutionContext(String executionNo, String capability, String promptKey,
                                 String promptVersion, String providerCode, String modelCode,
                                 String tenantKey, String principalKey, String requestId,
                                 String traceId) {
    public static AiExecutionContext of(String capability, String providerCode, String modelCode) {
        return new AiExecutionContext(null, capability, null, null, providerCode, modelCode,
                null, null, null, null);
    }

    /** Creates the single allowed structured-output repair attempt correlation. */
    public AiExecutionContext repairAttempt() {
        return new AiExecutionContext(suffix(executionNo), capability, promptKey, promptVersion,
                providerCode, modelCode, tenantKey, principalKey, suffix(requestId), traceId);
    }

    private static String suffix(String value) {
        return value == null || value.isBlank() ? value : value + "-repair";
    }
}
