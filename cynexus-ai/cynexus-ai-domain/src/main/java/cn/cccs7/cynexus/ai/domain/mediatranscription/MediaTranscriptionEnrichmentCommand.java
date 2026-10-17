package cn.cccs7.cynexus.ai.domain.mediatranscription;

public record MediaTranscriptionEnrichmentCommand(
        String transcript,
        String title,
        String platform,
        Integer duration,
        String preferredModelCode,
        String tenantKey,
        String principalKey) {
    public MediaTranscriptionEnrichmentCommand(String transcript, String title, String platform,
                                                Integer duration, String preferredModelCode) {
        this(transcript, title, platform, duration, preferredModelCode, null, null);
    }
}
