package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record PortfolioCandidateRecord(long id, long taskId, int rowNo, JsonNode normalizedPayload,
                                       String reviewStatus, long version, Double confidence, List<String> warnings) {
}
