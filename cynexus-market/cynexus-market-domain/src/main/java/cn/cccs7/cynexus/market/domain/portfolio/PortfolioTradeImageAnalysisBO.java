package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.databind.JsonNode;

public record PortfolioTradeImageAnalysisBO(String scenarioCode, String status, JsonNode content) {
}
