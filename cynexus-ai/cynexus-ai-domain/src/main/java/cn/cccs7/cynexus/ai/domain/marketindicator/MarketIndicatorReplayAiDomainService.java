package cn.cccs7.cynexus.ai.domain.marketindicator;

import java.util.Map;

public interface MarketIndicatorReplayAiDomainService {
    String CAPABILITY = "market_indicator_replay_explanation";
    String PROMPT_KEY = CAPABILITY;

    Map<String, Object> explain(Map<String, Object> evidence);
}
