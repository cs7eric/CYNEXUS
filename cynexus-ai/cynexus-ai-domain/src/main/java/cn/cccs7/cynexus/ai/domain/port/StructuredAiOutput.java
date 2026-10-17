package cn.cccs7.cynexus.ai.domain.port;

/** Framework-neutral normalized structured response and bounded diagnostics. */
public record StructuredAiOutput(String json, boolean repaired, String error) {
    public boolean successful() { return json != null && !json.isBlank() && error == null; }
}
