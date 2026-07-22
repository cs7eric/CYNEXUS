package cn.cccs7.cynexus.ai.domain.agent;

import java.util.Locale;
import java.util.Set;

public final class AgentFeedbackPolicy {
    private static final Set<String> LABELS = Set.of("CORRECT", "PARTIAL", "INCORRECT", "UNVERIFIED");

    private AgentFeedbackPolicy() {
    }

    public static AgentFeedbackBO normalize(AgentFeedbackBO feedback) {
        if (feedback == null || feedback.getRating() == null || feedback.getRating() < 1 || feedback.getRating() > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
        feedback.setFeedbackReason(limit(redact(feedback.getFeedbackReason()), 120));
        feedback.setCorrectionText(limit(redact(feedback.getCorrectionText()), 2_000));
        String label = feedback.getEvaluationLabel() == null ? "UNVERIFIED"
                : feedback.getEvaluationLabel().trim().toUpperCase(Locale.ROOT);
        if (!LABELS.contains(label)) throw new IllegalArgumentException("evaluationLabel is not supported");
        feedback.setEvaluationLabel(label);
        return feedback;
    }

    private static String limit(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static String redact(String value) {
        if (value == null) return null;
        return value.replaceAll("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer [REDACTED]")
                .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "[REDACTED_EMAIL]")
                .replaceAll("(?<!\\d)1[3-9]\\d{9}(?!\\d)", "[REDACTED_MOBILE]");
    }
}
