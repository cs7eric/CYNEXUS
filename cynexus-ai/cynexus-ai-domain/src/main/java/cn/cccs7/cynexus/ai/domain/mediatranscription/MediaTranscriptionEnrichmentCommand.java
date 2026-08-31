package cn.cccs7.cynexus.ai.domain.mediatranscription;

public record MediaTranscriptionEnrichmentCommand(
        String transcript,
        String title,
        String platform,
        Integer duration,
        String preferredModelCode) {
}
