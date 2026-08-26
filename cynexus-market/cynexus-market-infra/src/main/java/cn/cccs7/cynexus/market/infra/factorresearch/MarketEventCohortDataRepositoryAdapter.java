package cn.cccs7.cynexus.market.infra.factorresearch;

import cn.cccs7.cynexus.market.domain.factorresearch.cohort.MarketEventCohortDataRepository;
import cn.cccs7.cynexus.market.infra.factorresearch.entity.MarketFactorEventFactPO;
import cn.cccs7.cynexus.market.infra.factorresearch.mapper.MarketFactorCohortMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MarketEventCohortDataRepositoryAdapter implements MarketEventCohortDataRepository {
    private static final String SOURCE = "postgres.trade_review_security_daily_fact";
    private final MarketFactorCohortMapper mapper;
    public MarketEventCohortDataRepositoryAdapter(MarketFactorCohortMapper mapper) { this.mapper = mapper; }

    @Override public List<IntradayEventFact> listLimitUpTouches(LocalDate startDate, LocalDate endDate, int limit) {
        return mapper.listLimitUpTouches(startDate, endDate, limit).stream().map(value -> new IntradayEventFact(value.getCode(),
                value.getMarket(), value.getTradeDate(), value.getFirstLimitUpAt(), Boolean.TRUE.equals(value.getFinalLimitUp()),
                value.getChangePct(), SOURCE)).toList();
    }

    @Override public List<DailyMoveFact> listLargeMoveCloses(LocalDate startDate, LocalDate endDate, BigDecimal minimum,
                                                               BigDecimal maximum, int limit) {
        return mapper.listLargeMoveCloses(startDate, endDate, minimum, maximum, limit).stream().map(value -> new DailyMoveFact(
                value.getCode(), value.getMarket(), value.getTradeDate(), value.getChangePct(), SOURCE)).toList();
    }
}
