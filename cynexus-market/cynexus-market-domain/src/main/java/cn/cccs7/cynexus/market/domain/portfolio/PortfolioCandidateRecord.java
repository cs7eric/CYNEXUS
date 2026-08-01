package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.databind.JsonNode;

public record PortfolioCandidateRecord(long id, long taskId, int rowNo, JsonNode normalizedPayload,
                                       String reviewStatus, long version) {
}
