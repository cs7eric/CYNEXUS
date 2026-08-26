package cn.cccs7.cynexus.market.infra.factorresearch;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudySnapshotRepository;
import cn.cccs7.cynexus.market.infra.factorresearch.mapper.MarketFactorStudyMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import cn.cccs7.cynexus.market.domain.factorresearch.outcome.MarketFactorOutcomeEvaluator;
import org.springframework.stereotype.Repository;

@Repository
public class MarketFactorStudySnapshotRepositoryAdapter implements MarketFactorStudySnapshotRepository {
    private final MarketFactorStudyMapper mapper;

    public MarketFactorStudySnapshotRepositoryAdapter(MarketFactorStudyMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Snapshot> findPreEventUniverse(LocalDate eventDate, OffsetDateTime dataCutoffAt, String featureVersion) {
        return mapper.findPreEventUniverse(eventDate, dataCutoffAt, featureVersion).stream()
                .map(value -> new Snapshot(value.getCode(), value.getMarket(), value.getFeatureTradeDate(), value.getAvailableAt(),
                        value.getIndustry(), Boolean.TRUE.equals(value.getSt()), value.getFloatMarketCap(), value.getAmount(),
                        value.getSource(), value.getCoverage(), value.getMarketRegime(), value.getIndustryRegime()))
                .toList();
    }

    @Override
    public List<DailyPrimitivePoint> findDailyPrimitives(String code, String market, LocalDate eventDate, int sessions,
                                                           OffsetDateTime dataCutoffAt, String featureVersion) {
        return mapper.findDailyPrimitives(code, market, eventDate, sessions, dataCutoffAt, featureVersion).stream()
                .map(value -> new DailyPrimitivePoint(value.getFeatureTradeDate(), value.getAvailableAt(), primitives(value), value.getSource(), value.getCoverage()))
                .toList();
    }

    @Override
    public OutcomeSeries findOutcomeSeries(String code, String market, LocalDate eventDate, int sessions,
                                            OffsetDateTime dataCutoffAt, String featureVersion) {
        List<cn.cccs7.cynexus.market.infra.factorresearch.entity.MarketFactorStudySnapshotPO> rows =
                mapper.findOutcomeSeries(code, market, eventDate, sessions + 1, dataCutoffAt, featureVersion);
        if (rows.isEmpty() || rows.getFirst().getClose() == null || rows.getFirst().getBenchmarkClose() == null) return null;
        return new OutcomeSeries(rows.getFirst().getClose(), rows.getFirst().getBenchmarkClose(), rows.stream().skip(1)
                .filter(value -> value.getClose() != null && value.getBenchmarkClose() != null)
                .map(value -> new MarketFactorOutcomeEvaluator.DailyClose(value.getFeatureTradeDate(), value.getClose(), value.getBenchmarkClose())).toList());
    }

    private Map<String, java.math.BigDecimal> primitives(cn.cccs7.cynexus.market.infra.factorresearch.entity.MarketFactorStudySnapshotPO value) {
        Map<String, java.math.BigDecimal> primitives = new LinkedHashMap<>();
        add(primitives, "stock.close", value.getClose()); add(primitives, "stock.volume", value.getVolume());
        add(primitives, "stock.amount", value.getAmount()); add(primitives, "stock.turnover_pct", value.getTurnoverPct());
        add(primitives, "stock.return_1d", value.getReturn1d());
        add(primitives, "stock.return_3d", value.getReturn3d()); add(primitives, "stock.return_5d", value.getReturn5d());
        add(primitives, "stock.return_10d", value.getReturn10d()); add(primitives, "stock.return_20d", value.getReturn20d());
        add(primitives, "stock.return_60d", value.getReturn60d()); add(primitives, "stock.return_120d", value.getReturn120d());
        add(primitives, "stock.return_252d", value.getReturn252d());
        add(primitives, "stock.ma5", value.getMa5()); add(primitives, "stock.ma10", value.getMa10());
        add(primitives, "stock.ma20", value.getMa20()); add(primitives, "stock.ma60", value.getMa60());
        add(primitives, "stock.close_vs_ma5", value.getCloseVsMa5()); add(primitives, "stock.close_vs_ma10", value.getCloseVsMa10());
        add(primitives, "stock.close_vs_ma20", value.getCloseVsMa20()); add(primitives, "stock.close_vs_ma60", value.getCloseVsMa60());
        add(primitives, "stock.volume_vs_ma5", value.getVolumeVsMa5()); add(primitives, "stock.volume_vs_ma10", value.getVolumeVsMa10());
        add(primitives, "stock.volume_vs_ma20", value.getVolumeVsMa20()); add(primitives, "stock.volume_vs_ma60", value.getVolumeVsMa60());
        return primitives;
    }

    private void add(Map<String, java.math.BigDecimal> values, String code, java.math.BigDecimal value) {
        if (value != null) values.put(code, value);
    }
}
