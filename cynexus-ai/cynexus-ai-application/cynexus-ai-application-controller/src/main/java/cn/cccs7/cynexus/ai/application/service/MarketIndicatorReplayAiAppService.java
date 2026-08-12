package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.marketindicator.MarketIndicatorReplayAiDomainService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MarketIndicatorReplayAiAppService {
    private final MarketIndicatorReplayAiDomainService domainService;

    public MarketIndicatorReplayAiAppService(MarketIndicatorReplayAiDomainService domainService) {
        this.domainService = domainService;
    }

    public Map<String, Object> explain(Map<String, Object> evidence) {
        return domainService.explain(evidence);
    }
}
