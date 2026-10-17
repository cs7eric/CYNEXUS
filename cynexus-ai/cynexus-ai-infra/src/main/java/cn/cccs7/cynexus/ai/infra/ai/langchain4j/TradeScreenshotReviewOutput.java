package cn.cccs7.cynexus.ai.infra.ai.langchain4j;
import java.util.List;
public record TradeScreenshotReviewOutput(Boolean consistent, Double confidence, List<String> issues) { }
