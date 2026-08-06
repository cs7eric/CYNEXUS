package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PortfolioTradeImageAnalysisDomainService {
    private static final String SCENARIO_CODE = "portfolio_trade_screenshot_parse";
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final ObjectMapper objectMapper;
    private final PortfolioTradeVisionGateway visionGateway;

    public PortfolioTradeImageAnalysisDomainService(ObjectMapper objectMapper,
                                                    PortfolioTradeVisionGateway visionGateway) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.visionGateway = Objects.requireNonNull(visionGateway, "visionGateway");
    }

    public PortfolioTradeImageAnalysisBO analyze(String fileName, String contentType, byte[] content) {
        validate(contentType, content);
        try {
            JsonNode root = objectMapper.readTree(visionGateway.parse(fileName, contentType, content));
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("AI trade recognition result must be a JSON object");
            }
            JsonNode candidates = root.get("candidates");
            if (candidates == null || !candidates.isArray()) {
                throw new IllegalArgumentException("AI trade recognition result must contain candidates");
            }
            return new PortfolioTradeImageAnalysisBO(SCENARIO_CODE, "SUCCEEDED", root);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("AI trade recognition result is invalid JSON", exception);
        }
    }

    private static void validate(String contentType, byte[] content) {
        if (content == null || content.length == 0 || content.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("trade screenshot must be between 1 byte and 10 MiB");
        }
        if (!"image/png".equalsIgnoreCase(contentType)
                && !"image/jpeg".equalsIgnoreCase(contentType)
                && !"image/webp".equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("trade screenshot must be PNG, JPEG or WebP");
        }
    }
}
