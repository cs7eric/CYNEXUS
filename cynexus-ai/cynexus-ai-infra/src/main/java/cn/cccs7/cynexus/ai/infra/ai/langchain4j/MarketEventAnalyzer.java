package cn.cccs7.cynexus.ai.infra.ai.langchain4j;
import dev.langchain4j.service.UserMessage;
public interface MarketEventAnalyzer {
    @UserMessage("{{text}}") MarketEventAnalysisOutput analyze(String text);
}
