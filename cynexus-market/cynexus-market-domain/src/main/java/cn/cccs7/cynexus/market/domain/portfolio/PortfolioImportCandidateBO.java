package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record PortfolioImportCandidateBO(
        long taskId,
        int rowNo,
        JsonNode rawPayload,
        JsonNode normalizedPayload,
        Double confidence,
        List<String> warnings
) {
    public PortfolioImportCandidateBO {
        if (taskId <= 0 || rowNo <= 0 || rawPayload == null || normalizedPayload == null
                || confidence != null && (confidence < 0 || confidence > 1) || warnings == null) {
            throw new IllegalArgumentException("invalid portfolio import candidate");
        }
        warnings = List.copyOf(warnings);
    }
}
