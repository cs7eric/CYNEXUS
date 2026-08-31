package cn.cccs7.cynexus.ai.domain.entity;

import java.util.List;

public record MediaTranscriptionEnrichmentBO(
        String polishedText,
        String summary,
        String shortSummary,
        List<String> keyPoints,
        List<String> topics,
        List<String> methodology,
        List<String> actionItems,
        List<String> risks,
        String title,
        List<String> categories,
        List<String> tags) {
}
