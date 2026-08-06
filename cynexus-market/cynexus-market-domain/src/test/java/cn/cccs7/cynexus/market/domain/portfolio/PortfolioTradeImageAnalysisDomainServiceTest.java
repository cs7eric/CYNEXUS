package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class PortfolioTradeImageAnalysisDomainServiceTest {
    @Test
    void validatesAndReturnsStructuredTradeRecognitionContent() {
        PortfolioTradeImageAnalysisDomainService service = new PortfolioTradeImageAnalysisDomainService(
                new ObjectMapper(), new PortfolioTradeVisionGateway() {
                    @Override
                    public String parse(PortfolioImportAssetBO asset) {
                        return "";
                    }

                    @Override
                    public String parse(String fileName, String contentType, byte[] content) {
                        return "{\"isTradeScreenshot\":true,\"candidates\":[{\"symbol\":\"600519\"}]}";
                    }
                });

        PortfolioTradeImageAnalysisBO result = service.analyze("trade.png", "image/png", new byte[]{1});

        assertEquals("portfolio_trade_screenshot_parse", result.scenarioCode());
        assertEquals("SUCCEEDED", result.status());
        assertEquals(1, result.content().path("candidates").size());
    }

    @Test
    void rejectsUnsupportedImageAndMalformedRecognitionResult() {
        PortfolioTradeImageAnalysisDomainService service = new PortfolioTradeImageAnalysisDomainService(
                new ObjectMapper(), new PortfolioTradeVisionGateway() {
                    @Override
                    public String parse(PortfolioImportAssetBO asset) {
                        return "";
                    }

                    @Override
                    public String parse(String fileName, String contentType, byte[] content) {
                        return "not-json";
                    }
                });

        IllegalArgumentException unsupported = assertThrows(IllegalArgumentException.class,
                () -> service.analyze("trade.gif", "image/gif", new byte[]{1}));
        assertEquals(true, unsupported.getMessage().contains("PNG, JPEG or WebP"));
        IllegalStateException malformed = assertThrows(IllegalStateException.class,
                () -> service.analyze("trade.png", "image/png", new byte[]{1}));
        assertEquals(true, malformed.getMessage().contains("invalid JSON"));
    }
}
