package cn.cccs7.cynexus.ai.infra.ai.langchain4j;
import dev.langchain4j.data.message.UserMessage;
public interface TradeScreenshotAnalyzer {
    @dev.langchain4j.service.UserMessage TradeScreenshotOutput analyze(UserMessage message);
}
