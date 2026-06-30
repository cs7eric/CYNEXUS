package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import cn.cccs7.cynexus.market.domain.repository.IndustryApplicationRepository;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationConfigPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationResultPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationTargetPO;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorFundFlowDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.service.IndustryApplicationBasicService;
import cn.cccs7.cynexus.market.infra.basic.service.SectorFundFlowDailySnapshotBasicService;
import cn.cccs7.cynexus.market.infra.client.TdxLocalDayKlineClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class IndustryApplicationRepositoryImpl implements IndustryApplicationRepository {

    private static final List<IndustryApplicationBO.TargetCode> TARGET_CODES = buildTargetCodes();
    private static final String FUND_FLOW_SCOPE = "industry";
    private static final int FUND_FLOW_FULL_LIMIT = 500;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<IndustryApplicationBO.Bar>> BAR_LIST_TYPE = new TypeReference<>() {
    };

    private final IndustryApplicationBasicService industryApplicationBasicService;
    private final SectorFundFlowDailySnapshotBasicService sectorFundFlowDailySnapshotBasicService;
    private final TdxLocalDayKlineClient tdxLocalDayKlineClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public IndustryApplicationRepositoryImpl(IndustryApplicationBasicService industryApplicationBasicService,
                                             SectorFundFlowDailySnapshotBasicService sectorFundFlowDailySnapshotBasicService,
                                             TdxLocalDayKlineClient tdxLocalDayKlineClient) {
        this.industryApplicationBasicService = industryApplicationBasicService;
        this.sectorFundFlowDailySnapshotBasicService = sectorFundFlowDailySnapshotBasicService;
        this.tdxLocalDayKlineClient = tdxLocalDayKlineClient;
    }

    @Override
    public IndustryApplicationBO.Config getConfig() {
        IndustryApplicationConfigPO config = industryApplicationBasicService.getOrCreateConfig();
        return IndustryApplicationBO.Config.builder()
                .version(config.getVersion())
                .updatedAt(config.getUpdatedAt())
                .horizonDays(config.getHorizonDays())
                .horizonSegments(config.getHorizonSegments())
                .items(industryApplicationBasicService.listTargets().stream().map(this::toTarget).toList())
                .build();
    }

    @Override
    public IndustryApplicationBO.Config saveConfig(IndustryApplicationBO.Config config) {
        IndustryApplicationConfigPO current = industryApplicationBasicService.getOrCreateConfig();
        current.setVersion(config.getVersion() == null ? 1 : config.getVersion());
        current.setHorizonDays(config.getHorizonDays() == null ? 120 : Math.max(30, config.getHorizonDays()));
        current.setHorizonSegments(config.getHorizonSegments() == null ? 4 : Math.max(1, config.getHorizonSegments()));
        current.setSource("java-api");
        industryApplicationBasicService.saveConfig(current);

        List<IndustryApplicationTargetPO> targets = new ArrayList<>();
        List<IndustryApplicationBO.Target> incoming = config.getItems() == null ? List.of() : config.getItems();
        for (int i = 0; i < incoming.size(); i++) {
            targets.add(toTargetPO(incoming.get(i), i));
        }
        industryApplicationBasicService.replaceTargets(targets);
        return getConfig();
    }

    @Override
    public List<IndustryApplicationBO.TargetCode> listTargetCodes(String kind) {
        String normalized = kind == null ? "" : kind.trim().toLowerCase();
        if (!List.of("industry", "concept").contains(normalized)) {
            return TARGET_CODES;
        }
        return TARGET_CODES.stream().filter(item -> normalized.equals(item.getKind())).toList();
    }

    @Override
    public Optional<IndustryApplicationBO.Target> findTarget(String targetKey) {
        return industryApplicationBasicService.findTarget(targetKey).map(this::toTarget);
    }

    @Override
    public IndustryApplicationBO.Kline getKline(String targetType, String symbol, String period, int count) {
        String normalizedType = normalizeTargetType(targetType, symbol);
        String normalizedSymbol = normalizeSymbol(symbol);
        int normalizedCount = Math.max(1, Math.min(count <= 0 ? 120 : count, 2000));
        List<IndustryApplicationBO.Bar> bars = tdxLocalDayKlineClient.readDailyBars(normalizedSymbol, normalizedCount);
        return IndustryApplicationBO.Kline.builder()
                .targetType(normalizedType)
                .code(normalizedSymbol)
                .name(lookupName(normalizedType, normalizedSymbol))
                .period(normalizePeriod(period))
                .bars(bars)
                .indicators(computeIndicators(bars))
                .fetchedAt(OffsetDateTime.now())
                .source("tdx.local_day")
                .build();
    }

    @Override
    public Optional<IndustryApplicationBO.Result> findResult(String targetKey) {
        return industryApplicationBasicService.findResult(targetKey).map(this::toResult);
    }

    @Override
    public void saveResult(IndustryApplicationBO.Target target, IndustryApplicationBO.Kline kline) {
        IndustryApplicationResultPO value = new IndustryApplicationResultPO();
        value.setTargetKey(target.getTargetKey());
        value.setTargetType(target.getTargetType());
        value.setSymbol(target.getSymbol());
        value.setName(target.getName());
        value.setUpdatedAtResult(kline.getFetchedAt());
        value.setKline(toJson(kline.getBars()));
        value.setIndicators(toJson(kline.getIndicators()));
        IndustryApplicationBO.Result meta = IndustryApplicationBO.Result.builder()
                .source(kline.getSource())
                .period(kline.getPeriod())
                .bars(kline.getBars())
                .build();
        value.setMeta(toJson(meta));
        value.setSource(kline.getSource());
        industryApplicationBasicService.saveResult(value);
    }

    @Override
    public List<IndustryApplicationBO.SectorOverviewItem> listOverview(String sortBy, boolean ascending, int count) {
        List<IndustryApplicationBO.SectorOverviewItem> items = TARGET_CODES.stream()
                .map(code -> toOverviewItem(code, tdxLocalDayKlineClient.readDailyBars(code.getCode(), 2)))
                .filter(Objects::nonNull)
                .sorted(comparator(sortBy, ascending))
                .toList();
        int normalizedCount = Math.max(1, Math.min(count <= 0 ? 200 : count, 1000));
        return items.size() > normalizedCount ? items.subList(0, normalizedCount) : items;
    }

    @Override
    public IndustryApplicationBO.FundFlowSnapshot getFundFlow(LocalDate tradeDate, Integer top) {
        LocalDate resolvedTradeDate = sectorFundFlowDailySnapshotBasicService.findLatestTradeDate(FUND_FLOW_SCOPE, tradeDate);
        if (resolvedTradeDate == null) {
            return IndustryApplicationBO.FundFlowSnapshot.builder()
                    .ok(false)
                    .tradeDate(tradeDate)
                    .rowCount(0)
                    .totalPages(0)
                    .pageRowCounts(List.of())
                    .rows(List.of())
                    .source("sector_fund_flow_daily_snapshots")
                    .error("no industry fund-flow snapshot found")
                    .build();
        }

        List<SectorFundFlowDailySnapshotPO> allRows = sectorFundFlowDailySnapshotBasicService.listByTradeDate(
                FUND_FLOW_SCOPE,
                resolvedTradeDate,
                FUND_FLOW_FULL_LIMIT
        );
        int normalizedTop = top == null ? 0 : Math.max(0, Math.min(top, FUND_FLOW_FULL_LIMIT));
        List<SectorFundFlowDailySnapshotPO> visibleRows = normalizedTop > 0 && allRows.size() > normalizedTop
                ? allRows.subList(0, normalizedTop)
                : allRows;
        boolean stale = tradeDate != null && resolvedTradeDate.isBefore(tradeDate);
        return IndustryApplicationBO.FundFlowSnapshot.builder()
                .ok(!visibleRows.isEmpty())
                .tradeDate(resolvedTradeDate)
                .rowCount(visibleRows.size())
                .totalPages((allRows.size() + 49) / 50)
                .pageRowCounts(pageRowCounts(allRows.size()))
                .fetchedAt(allRows.stream()
                        .map(SectorFundFlowDailySnapshotPO::getCapturedAt)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null))
                .rows(visibleRows.stream().map(this::toFundFlowRow).toList())
                .source(allRows.stream()
                        .map(SectorFundFlowDailySnapshotPO::getSource)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("sector_fund_flow_daily_snapshots"))
                .stale(stale)
                .staleReason(stale ? "requested trade date has no snapshot; using latest previous snapshot" : null)
                .error(visibleRows.isEmpty() ? "no industry fund-flow rows found" : null)
                .build();
    }

    @Override
    public IndustryApplicationBO.FundFlowHistory getFundFlowHistory(LocalDate maxTradeDate, int days, Integer top) {
        int normalizedDays = Math.max(1, Math.min(days <= 0 ? 365 : days, 500));
        int normalizedTop = Math.max(1, Math.min(top == null || top <= 0 ? FUND_FLOW_FULL_LIMIT : top, FUND_FLOW_FULL_LIMIT));
        List<SectorFundFlowDailySnapshotPO> rows = sectorFundFlowDailySnapshotBasicService.listRecent(
                FUND_FLOW_SCOPE,
                maxTradeDate,
                normalizedDays,
                normalizedTop
        );
        List<LocalDate> dates = rows.stream()
                .map(SectorFundFlowDailySnapshotPO::getTradeDate)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        return IndustryApplicationBO.FundFlowHistory.builder()
                .ok(!rows.isEmpty())
                .days(normalizedDays)
                .dates(dates)
                .rowCount(rows.size())
                .fetchedAt(rows.stream()
                        .map(SectorFundFlowDailySnapshotPO::getCapturedAt)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null))
                .rows(rows.stream().map(this::toFundFlowRow).toList())
                .source(rows.stream()
                        .map(SectorFundFlowDailySnapshotPO::getSource)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("sector_fund_flow_daily_snapshots"))
                .error(rows.isEmpty() ? "no industry fund-flow history found" : null)
                .build();
    }

    private IndustryApplicationBO.Target toTarget(IndustryApplicationTargetPO po) {
        return IndustryApplicationBO.Target.builder()
                .id(po.getId())
                .targetKey(po.getTargetKey())
                .targetType(po.getTargetType())
                .symbol(po.getSymbol())
                .name(po.getName())
                .enabled(po.getEnabled())
                .intervalMinutes(po.getIntervalMinutes())
                .tags(readTags(po.getTags()))
                .displayOrder(po.getDisplayOrder())
                .build();
    }

    private IndustryApplicationTargetPO toTargetPO(IndustryApplicationBO.Target value, int order) {
        String targetType = normalizeTargetType(value.getTargetType(), value.getSymbol());
        String symbol = normalizeSymbol(value.getSymbol());
        IndustryApplicationTargetPO po = new IndustryApplicationTargetPO();
        po.setTargetKey(targetKey(targetType, symbol, value.getTargetKey()));
        po.setTargetType(targetType);
        po.setSymbol(symbol);
        po.setName(value.getName() == null || value.getName().isBlank() ? lookupName(targetType, symbol) : value.getName());
        po.setEnabled(value.getEnabled() == null || value.getEnabled());
        po.setIntervalMinutes(value.getIntervalMinutes() == null ? 60 : Math.max(5, value.getIntervalMinutes()));
        po.setTags(toJson(value.getTags() == null ? List.of() : value.getTags()));
        po.setDisplayOrder(order);
        po.setSource("java-api");
        return po;
    }

    private IndustryApplicationBO.Result toResult(IndustryApplicationResultPO po) {
        String source = po.getSource();
        String period = "day";
        try {
            IndustryApplicationBO.Result meta = objectMapper.readValue(po.getMeta(), IndustryApplicationBO.Result.class);
            source = meta.getSource() == null ? source : meta.getSource();
            period = meta.getPeriod() == null ? period : meta.getPeriod();
        } catch (Exception ignored) {
            // Keep persisted result readable even if old meta is incomplete.
        }
        return IndustryApplicationBO.Result.builder()
                .target(IndustryApplicationBO.Target.builder()
                        .targetKey(po.getTargetKey())
                        .targetType(po.getTargetType())
                        .symbol(po.getSymbol())
                        .name(po.getName())
                        .tags(List.of())
                        .enabled(true)
                        .intervalMinutes(60)
                        .build())
                .updatedAt(po.getUpdatedAtResult())
                .bars(readBars(po.getKline()))
                .indicators(readIndicators(po.getIndicators()))
                .source(source)
                .period(period)
                .build();
    }

    private IndustryApplicationBO.SectorOverviewItem toOverviewItem(IndustryApplicationBO.TargetCode code,
                                                                    List<IndustryApplicationBO.Bar> bars) {
        if (bars == null || bars.isEmpty()) {
            return null;
        }
        IndustryApplicationBO.Bar latest = last(bars);
        BigDecimal change = latest.getPrevClose() == null ? null : latest.getClose().subtract(latest.getPrevClose());
        return IndustryApplicationBO.SectorOverviewItem.builder()
                .fullCode(code.getCode())
                .name(code.getName())
                .kind(code.getKind())
                .lastPrice(latest.getClose())
                .preClosePrice(latest.getPrevClose())
                .openPrice(latest.getOpen())
                .highPrice(latest.getHigh())
                .lowPrice(latest.getLow())
                .change(change)
                .changePct(latest.getPct())
                .amplitudePct(percent(latest.getHigh().subtract(latest.getLow()), latest.getPrevClose()))
                .highPct(percent(latest.getHigh().subtract(nullToZero(latest.getPrevClose())), latest.getPrevClose()))
                .lowPct(percent(latest.getLow().subtract(nullToZero(latest.getPrevClose())), latest.getPrevClose()))
                .openPct(percent(latest.getOpen().subtract(nullToZero(latest.getPrevClose())), latest.getPrevClose()))
                .volume(latest.getVolumeLots())
                .amount(latest.getAmount())
                .tradingDate(latest.getTradeDate())
                .build();
    }

    private IndustryApplicationBO.FundFlowRow toFundFlowRow(SectorFundFlowDailySnapshotPO po) {
        return IndustryApplicationBO.FundFlowRow.builder()
                .rank(po.getRank())
                .industry(po.getSectorName())
                .code(po.getSectorCode())
                .changePct(po.getChangePct())
                .inflow(po.getInflow())
                .outflow(po.getOutflow())
                .net(po.getNet())
                .companyCount(po.getCompanyCount())
                .leaderStock(po.getLeaderStock())
                .leaderChange(po.getLeaderChange())
                .leaderPrice(po.getLeaderPrice())
                .tradeDate(po.getTradeDate())
                .source(po.getSource())
                .build();
    }

    private List<Integer> pageRowCounts(int rowCount) {
        if (rowCount <= 0) {
            return List.of();
        }
        List<Integer> counts = new ArrayList<>();
        int remaining = rowCount;
        while (remaining > 0) {
            int current = Math.min(50, remaining);
            counts.add(current);
            remaining -= current;
        }
        return counts;
    }

    private IndustryApplicationBO.Indicators computeIndicators(List<IndustryApplicationBO.Bar> bars) {
        if (bars == null || bars.isEmpty()) {
            return IndustryApplicationBO.Indicators.builder().barCount(0).build();
        }
        IndustryApplicationBO.Bar latest = last(bars);
        List<BigDecimal> closes = bars.stream().map(IndustryApplicationBO.Bar::getClose).toList();
        BigDecimal ma20 = ma(closes, 20);
        BigDecimal ma60 = ma(closes, 60);
        BigDecimal ma120 = ma(closes, 120);
        BigDecimal ma250 = ma(closes, 250);
        BigDecimal high20 = bars.size() < 20 ? null : bars.subList(bars.size() - 20, bars.size()).stream()
                .map(IndustryApplicationBO.Bar::getHigh).max(Comparator.naturalOrder()).orElse(null);
        BigDecimal low20 = bars.size() < 20 ? null : bars.subList(bars.size() - 20, bars.size()).stream()
                .map(IndustryApplicationBO.Bar::getLow).min(Comparator.naturalOrder()).orElse(null);
        return IndustryApplicationBO.Indicators.builder()
                .latestClose(latest.getClose())
                .latestPct(latest.getPct())
                .latestTime(latest.getTime())
                .ma20(ma20)
                .ma60(ma60)
                .ma120(ma120)
                .ma250(ma250)
                .aboveMa20(ma20 != null && latest.getClose().compareTo(ma20) > 0)
                .aboveMa60(ma60 != null && latest.getClose().compareTo(ma60) > 0)
                .aboveMa20Pct(percent(latest.getClose().subtract(nullToZero(ma20)), ma20))
                .aboveMa60Pct(percent(latest.getClose().subtract(nullToZero(ma60)), ma60))
                .aboveMa20Streak(aboveMaStreak(bars, 20))
                .high20(high20)
                .low20(low20)
                .rangePos20(rangePosition(latest.getClose(), high20, low20))
                .return5d(periodReturn(closes, 5))
                .return20d(periodReturn(closes, 20))
                .return60d(periodReturn(closes, 60))
                .barCount(bars.size())
                .build();
    }

    private BigDecimal ma(List<BigDecimal> values, int window) {
        if (values.size() < window) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values.subList(values.size() - window, values.size())) {
            sum = sum.add(value);
        }
        return sum.divide(BigDecimal.valueOf(window), 8, RoundingMode.HALF_UP);
    }

    private int aboveMaStreak(List<IndustryApplicationBO.Bar> bars, int window) {
        int streak = 0;
        for (int i = bars.size() - 1; i >= 0; i--) {
            if (i + 1 < window) {
                break;
            }
            List<BigDecimal> closes = bars.subList(0, i + 1).stream().map(IndustryApplicationBO.Bar::getClose).toList();
            BigDecimal currentMa = ma(closes, window);
            if (currentMa == null || bars.get(i).getClose().compareTo(currentMa) < 0) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private BigDecimal periodReturn(List<BigDecimal> closes, int window) {
        if (closes.size() < window + 1) {
            return null;
        }
        BigDecimal previous = closes.get(closes.size() - 1 - window);
        BigDecimal current = closes.get(closes.size() - 1);
        return percent(current.subtract(previous), previous);
    }

    private BigDecimal rangePosition(BigDecimal close, BigDecimal high, BigDecimal low) {
        if (close == null || high == null || low == null || high.compareTo(low) == 0) {
            return null;
        }
        return close.subtract(low).divide(high.subtract(low), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private IndustryApplicationBO.Bar last(List<IndustryApplicationBO.Bar> bars) {
        return bars.get(bars.size() - 1);
    }

    private Comparator<IndustryApplicationBO.SectorOverviewItem> comparator(String sortBy, boolean ascending) {
        String normalized = sortBy == null ? "涨幅" : sortBy;
        Comparator<IndustryApplicationBO.SectorOverviewItem> comparator = switch (normalized) {
            case "amount", "成交额" -> Comparator.comparing(item -> nullToZero(item.getAmount()));
            case "name", "名称" -> Comparator.comparing(IndustryApplicationBO.SectorOverviewItem::getName);
            default -> Comparator.comparing(item -> nullToZero(item.getChangePct()));
        };
        return ascending ? comparator : comparator.reversed();
    }

    private String normalizePeriod(String period) {
        String value = period == null ? "day" : period.trim();
        return switch (value) {
            case "1d" -> "day";
            case "day" -> "day";
            default -> "day";
        };
    }

    private String normalizeTargetType(String targetType, String symbol) {
        String value = targetType == null ? "" : targetType.trim().toLowerCase();
        if ("industry".equals(value) || "concept".equals(value)) {
            return value;
        }
        return normalizeSymbol(symbol).startsWith("sh8804") ? "concept" : "industry";
    }

    private String normalizeSymbol(String symbol) {
        String value = symbol == null ? "" : symbol.trim().toLowerCase();
        if (value.matches("^[a-z]{2}\\d{6}$")) {
            return value;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 6) {
            return "sh" + digits;
        }
        throw new IllegalArgumentException("symbol is required");
    }

    private String targetKey(String targetType, String symbol, String preferred) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return targetType + "-" + symbol;
    }

    private String lookupName(String targetType, String symbol) {
        return TARGET_CODES.stream()
                .filter(item -> item.getKind().equals(targetType) && item.getCode().equals(symbol))
                .map(IndustryApplicationBO.TargetCode::getName)
                .findFirst()
                .orElse(symbol);
    }

    private List<String> readTags(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<IndustryApplicationBO.Bar> readBars(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, BAR_LIST_TYPE);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private IndustryApplicationBO.Indicators readIndicators(String json) {
        if (json == null || json.isBlank()) {
            return IndustryApplicationBO.Indicators.builder().barCount(0).build();
        }
        try {
            return objectMapper.readValue(json, IndustryApplicationBO.Indicators.class);
        } catch (Exception ex) {
            return IndustryApplicationBO.Indicators.builder().barCount(0).build();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize industry application value", ex);
        }
    }

    private static List<IndustryApplicationBO.TargetCode> buildTargetCodes() {
        List<IndustryApplicationBO.TargetCode> out = new ArrayList<>();
        add(out, "industry", "sh880301", "农副食品");
        add(out, "industry", "sh880302", "纺织服饰");
        add(out, "industry", "sh880303", "轻工制造");
        add(out, "industry", "sh880304", "医药生物");
        add(out, "industry", "sh880305", "公用事业");
        add(out, "industry", "sh880306", "交通运输");
        add(out, "industry", "sh880307", "房地产");
        add(out, "industry", "sh880308", "商贸零售");
        add(out, "industry", "sh880309", "社会服务");
        add(out, "industry", "sh880310", "综合");
        add(out, "industry", "sh880311", "建筑材料");
        add(out, "industry", "sh880312", "建筑装饰");
        add(out, "industry", "sh880313", "电力设备");
        add(out, "industry", "sh880314", "机械设备");
        add(out, "industry", "sh880315", "国防军工");
        add(out, "industry", "sh880316", "汽车");
        add(out, "industry", "sh880317", "美容护理");
        add(out, "industry", "sh880318", "家用电器");
        add(out, "industry", "sh880319", "食品饮料");
        add(out, "industry", "sh880320", "煤炭");
        add(out, "industry", "sh880321", "石油石化");
        add(out, "industry", "sh880322", "环保");
        add(out, "industry", "sh880323", "银行");
        add(out, "industry", "sh880324", "非银金融");
        add(out, "industry", "sh880325", "钢铁");
        add(out, "industry", "sh880326", "有色金属");
        add(out, "industry", "sh880327", "电子");
        add(out, "industry", "sh880328", "计算机");
        add(out, "industry", "sh880329", "传媒");
        add(out, "industry", "sh880330", "通信");
        add(out, "industry", "sh880331", "农林牧渔");
        add(out, "industry", "sh880332", "基础化工");
        add(out, "concept", "sh880401", "沪深300概念");
        add(out, "concept", "sh880402", "中证500概念");
        add(out, "concept", "sh880403", "中证1000概念");
        add(out, "concept", "sh880404", "中证2000概念");
        add(out, "concept", "sh880405", "红利概念");
        add(out, "concept", "sh880406", "高股息概念");
        add(out, "concept", "sh880407", "国企改革");
        add(out, "concept", "sh880408", "央企改革");
        add(out, "concept", "sh880409", "一带一路");
        add(out, "concept", "sh880410", "粤港澳大湾区");
        add(out, "concept", "sh880411", "长三角一体化");
        add(out, "concept", "sh880412", "雄安新区");
        add(out, "concept", "sh880413", "海南自贸港");
        add(out, "concept", "sh880414", "上海自贸");
        add(out, "concept", "sh880415", "数字货币");
        add(out, "concept", "sh880416", "区块链");
        add(out, "concept", "sh880417", "人工智能");
        add(out, "concept", "sh880418", "大数据");
        add(out, "concept", "sh880419", "云计算");
        add(out, "concept", "sh880420", "5G");
        add(out, "concept", "sh880421", "芯片");
        add(out, "concept", "sh880422", "国产芯片");
        add(out, "concept", "sh880423", "存储芯片");
        add(out, "concept", "sh880424", "半导体");
        add(out, "concept", "sh880425", "集成电路");
        add(out, "concept", "sh880426", "工业互联网");
        add(out, "concept", "sh880427", "智能制造");
        add(out, "concept", "sh880428", "卫星互联网");
        add(out, "concept", "sh880429", "低空经济");
        add(out, "concept", "sh880430", "人形机器人");
        add(out, "concept", "sh880431", "CPO");
        add(out, "concept", "sh880432", "数字经济");
        add(out, "concept", "sh880433", "新能源");
        add(out, "concept", "sh880434", "锂电池");
        add(out, "concept", "sh880435", "固态电池");
        add(out, "concept", "sh880436", "储能");
        add(out, "concept", "sh880437", "充电桩");
        add(out, "concept", "sh880438", "光伏");
        add(out, "concept", "sh880439", "风电");
        add(out, "concept", "sh880440", "核电");
        add(out, "concept", "sh880441", "氢能源");
        add(out, "concept", "sh880442", "特高压");
        add(out, "concept", "sh880443", "碳中和");
        add(out, "concept", "sh880444", "军工");
        add(out, "concept", "sh880445", "国产替代");
        add(out, "concept", "sh880446", "高端装备");
        add(out, "concept", "sh880447", "智慧城市");
        add(out, "concept", "sh880448", "乡村振兴");
        add(out, "concept", "sh880449", "医美");
        add(out, "concept", "sh880450", "创新药");
        return List.copyOf(out);
    }

    private static void add(List<IndustryApplicationBO.TargetCode> out, String kind, String code, String name) {
        out.add(IndustryApplicationBO.TargetCode.builder().kind(kind).code(code).name(name).build());
    }
}
