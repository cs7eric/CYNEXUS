package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.infra.ai.spring.SpringAiModelFactory;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Component;

/** Creates typed LangChain4j services without allowing them to route or persist business data. */
@Component
public class LangChain4jServiceFactory {
    private final SpringAiModelFactory models;
    public LangChain4jServiceFactory(SpringAiModelFactory models) { this.models = models; }

    public MediaTranscriptAnalyzer mediaTranscriptAnalyzer(AiModelBO model) {
        return create(MediaTranscriptAnalyzer.class, model);
    }

    public KnowledgeDocumentGenerator knowledgeDocumentGenerator(AiModelBO model) { return create(KnowledgeDocumentGenerator.class, model); }
    public MarketEventAnalyzer marketEventAnalyzer(AiModelBO model) { return create(MarketEventAnalyzer.class, model); }
    public FactorStudySynthesizer factorStudySynthesizer(AiModelBO model) { return create(FactorStudySynthesizer.class, model); }
    public FuturesPositionAnalyzer futuresPositionAnalyzer(AiModelBO model) { return create(FuturesPositionAnalyzer.class, model); }
    public MarketInformationAnalyzer marketInformationAnalyzer(AiModelBO model) { return create(MarketInformationAnalyzer.class, model); }
    public ApplicationAnalysisAnalyzer applicationAnalysisAnalyzer(AiModelBO model) { return create(ApplicationAnalysisAnalyzer.class, model); }
    public StockSelectionAnalyzer stockSelectionAnalyzer(AiModelBO model) { return create(StockSelectionAnalyzer.class, model); }
    public MarketIndicatorReplayAnalyzer marketIndicatorReplayAnalyzer(AiModelBO model) { return create(MarketIndicatorReplayAnalyzer.class, model); }
    public ReceiptVisionAnalyzer receiptVisionAnalyzer(AiModelBO model) { return create(ReceiptVisionAnalyzer.class, model); }
    public TradeScreenshotAnalyzer tradeScreenshotAnalyzer(AiModelBO model) { return create(TradeScreenshotAnalyzer.class, model); }
    public TradeScreenshotReviewAnalyzer tradeScreenshotReviewAnalyzer(AiModelBO model) { return create(TradeScreenshotReviewAnalyzer.class, model); }
    public WechatImageRouteAnalyzer wechatImageRouteAnalyzer(AiModelBO model) { return create(WechatImageRouteAnalyzer.class, model); }

    public GenericTextAnalyzer genericTextAnalyzer(AiModelBO model) { return create(GenericTextAnalyzer.class, model); }

    /** Exposes the framework bridge for governed multimodal calls. */
    public dev.langchain4j.model.chat.ChatModel chatModel(AiModelBO model) {
        return new SpringBackedLangChainChatModel(models.resolve(model));
    }

    private <T> T create(Class<T> type, AiModelBO model) {
        return AiServices.create(type, new SpringBackedLangChainChatModel(models.resolve(model)));
    }
}
