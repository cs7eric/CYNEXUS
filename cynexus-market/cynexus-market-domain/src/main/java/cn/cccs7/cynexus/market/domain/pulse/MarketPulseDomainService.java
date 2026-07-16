package cn.cccs7.cynexus.market.domain.pulse;

import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import java.time.LocalDate;
import java.util.List;

public interface MarketPulseDomainService {

    void applyServingFacts(MarketOverviewBO overview);

    void applyServingFacts(List<MarketOverviewBO> overviews);

    int historyDays(String range);

    int safeHistoryDays(int days);

    String normalizeDate(String value);

    String normalizeInterval(String value);

    List<String> defaultIndexCodes();

    String indexName(String code);

    String resolveServingMode(LocalDate tradeDate, String sourceKind);
}
