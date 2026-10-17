package cn.cccs7.cynexus.ai.infra.ai.langchain4j;
import dev.langchain4j.service.UserMessage;
public interface FuturesPositionAnalyzer {
    @UserMessage("{{text}}") FuturesPositionAnalysisOutput analyze(String text);
}
