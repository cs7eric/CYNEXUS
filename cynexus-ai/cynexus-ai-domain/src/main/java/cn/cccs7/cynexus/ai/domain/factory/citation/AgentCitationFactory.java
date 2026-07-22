package cn.cccs7.cynexus.ai.domain.factory.citation;

import cn.cccs7.cynexus.ai.domain.policy.sensitivedata.AgentSourceUrlPolicy;
import cn.cccs7.cynexus.ai.domain.valueobject.citation.AgentCitation;
import cn.cccs7.cynexus.ai.domain.valueobject.citation.AgentCitation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

public final class AgentCitationFactory {
    private AgentCitationFactory() {
    }

    public static List<AgentCitation> fromTool(String toolCode, String source, String sourceUrl,
                                               String dataDate, String summary, String freshness) {
        if (toolCode == null || toolCode.isBlank() || (source == null || source.isBlank())
                && (sourceUrl == null || sourceUrl.isBlank())) return List.of();
        String safeUrl = AgentSourceUrlPolicy.sanitize(sourceUrl, Set.of());
        String publisher = source == null || source.isBlank() ? "Cynexus" : source;
        String excerpt = summary == null ? "" : summary.trim();
        String canonical = String.join("|", toolCode, publisher, safeUrl == null ? "" : safeUrl,
                dataDate == null ? "" : dataDate, excerpt);
        return List.of(AgentCitation.builder()
                .citationId("cit-" + sha256(canonical).substring(0, 20))
                .title(publisher + " · " + toolCode)
                .url(safeUrl)
                .publisher(publisher)
                .publicationDate(dataDate)
                .retrievedAt(java.time.OffsetDateTime.now().toString())
                .dataDate(dataDate)
                .excerpt(excerpt.length() > 500 ? excerpt.substring(0, 500) : excerpt)
                .freshness(freshness)
                .sourceType("TOOL_RESULT")
                .availability(safeUrl == null ? "UNLINKED" : "AVAILABLE")
                .conflict(false)
                .build());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
