package cn.cccs7.cynexus.ai.infra.ai.langchain4j;
import java.util.List;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.JsonNode;
public record TradeScreenshotOutput(Boolean isTradeScreenshot, List<TradeCandidate> candidates, List<String> warnings) {
    public record TradeCandidate(Integer rowNo, String symbol, String securityName, String instrumentType,
                                 String side, String tradedAt, BigDecimal quantity, BigDecimal price,
                                 BigDecimal fee, String externalTradeNo, Double confidence,
                                 JsonNode evidence, List<String> warnings) { }
}
