package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseSectorDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.IndustryApplicationRepository;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import cn.cccs7.cynexus.market.domain.repository.TdxSectorQuoteDetailSnapshotRepository;
import cn.cccs7.cynexus.market.domain.repository.ThsIndustryConstituentSnapshotRepository;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationConfigPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationResultPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationTargetPO;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorFundFlowDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.TdxThsIndustryMappingPO;
import cn.cccs7.cynexus.market.infra.basic.service.IndustryApplicationBasicService;
import cn.cccs7.cynexus.market.infra.basic.service.SectorFundFlowDailySnapshotBasicService;
import cn.cccs7.cynexus.market.infra.basic.service.TdxThsIndustryMappingBasicService;
import cn.cccs7.cynexus.market.infra.client.ClickHouseStockChartClient;
import cn.cccs7.cynexus.market.infra.client.HttpIndustryApplicationDataClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class IndustryApplicationRepositoryImpl implements IndustryApplicationRepository {

    private static final String FUND_FLOW_SCOPE = "industry";
    private static final int FUND_FLOW_FULL_LIMIT = 500;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<IndustryApplicationBO.Bar>> BAR_LIST_TYPE = new TypeReference<>() {
    };

    private final IndustryApplicationBasicService industryApplicationBasicService;
    private final IndustryApplicationTargetCatalogProvider industryApplicationTargetCatalogProvider;
    private final SectorFundFlowDailySnapshotBasicService sectorFundFlowDailySnapshotBasicService;
    private final MarketPulseQueryRepository marketPulseQueryRepository;
    private final ClickHouseStockChartClient clickHouseStockChartClient;
    private final HttpIndustryApplicationDataClient industryApplicationDataClient;
    private final TdxThsIndustryMappingBasicService tdxThsIndustryMappingBasicService;
    private final ThsIndustryConstituentSnapshotRepository thsIndustryConstituentSnapshotRepository;
    private final TdxSectorQuoteDetailSnapshotRepository tdxSectorQuoteDetailSnapshotRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public IndustryApplicationRepositoryImpl(IndustryApplicationBasicService industryApplicationBasicService,
                                             IndustryApplicationTargetCatalogProvider industryApplicationTargetCatalogProvider,
                                             SectorFundFlowDailySnapshotBasicService sectorFundFlowDailySnapshotBasicService,
                                             MarketPulseQueryRepository marketPulseQueryRepository,
                                             ClickHouseStockChartClient clickHouseStockChartClient,
                                             HttpIndustryApplicationDataClient industryApplicationDataClient,
                                             TdxThsIndustryMappingBasicService tdxThsIndustryMappingBasicService,
                                             ThsIndustryConstituentSnapshotRepository thsIndustryConstituentSnapshotRepository,
                                             TdxSectorQuoteDetailSnapshotRepository tdxSectorQuoteDetailSnapshotRepository) {
        this.industryApplicationBasicService = industryApplicationBasicService;
        this.industryApplicationTargetCatalogProvider = industryApplicationTargetCatalogProvider;
        this.sectorFundFlowDailySnapshotBasicService = sectorFundFlowDailySnapshotBasicService;
        this.marketPulseQueryRepository = marketPulseQueryRepository;
        this.clickHouseStockChartClient = clickHouseStockChartClient;
        this.industryApplicationDataClient = industryApplicationDataClient;
        this.tdxThsIndustryMappingBasicService = tdxThsIndustryMappingBasicService;
        this.thsIndustryConstituentSnapshotRepository = thsIndustryConstituentSnapshotRepository;
        this.tdxSectorQuoteDetailSnapshotRepository = tdxSectorQuoteDetailSnapshotRepository;
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
        List<IndustryApplicationBO.TargetCode> targetCodes = currentTargetCodes();
        String normalized = kind == null ? "" : kind.trim().toLowerCase();
        if (!List.of("industry", "concept", "index").contains(normalized)) {
            return targetCodes;
        }
        return targetCodes.stream().filter(item -> normalized.equals(item.getKind())).toList();
    }

    @Override
    public Optional<IndustryApplicationBO.Target> findTarget(String targetKey) {
        return industryApplicationBasicService.findTarget(targetKey).map(this::toTarget);
    }

    @Override
    public IndustryApplicationBO.Kline getKline(String targetType, String symbol, String period, int count) {
        String normalizedType = normalizeTargetType(targetType, symbol);
        String normalizedSymbol = normalizeSymbol(normalizedType, symbol);
        int normalizedCount = Math.max(1, Math.min(count <= 0 ? 120 : count, 2000));
        List<IndustryApplicationBO.Bar> bars = loadBarsFromClickHouse(normalizedSymbol, normalizedCount);
        return IndustryApplicationBO.Kline.builder()
                .targetType(normalizedType)
                .code(normalizedSymbol)
                .name(lookupName(normalizedType, normalizedSymbol))
                .period(normalizePeriod(period))
                .bars(bars)
                .indicators(computeIndicators(bars))
                .fetchedAt(OffsetDateTime.now())
                .source("clickhouse.daily_raw")
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
        List<IndustryApplicationBO.TargetCode> targetCodes = currentTargetCodes();
        List<ClickHouseStockChartClient.MarketSymbol> symbols = targetCodes.stream()
                .filter(code -> !"index".equals(code.getKind()))
                .map(code -> toMarketSymbol(code.getCode()))
                .distinct()
                .toList();
        Map<String, ClickHouseStockChartClient.DailyQuote> quotesByCode = clickHouseStockChartClient.listLatestDailyQuotes(symbols).stream()
                .collect(Collectors.toMap(
                        quote -> toFullCode(quote.getMarket(), quote.getSymbol()),
                        quote -> quote,
                        (left, right) -> left
                ));
        List<IndustryApplicationBO.SectorOverviewItem> items = targetCodes.stream()
                .filter(code -> !"index".equals(code.getKind()))
                .map(code -> toOverviewItem(code, quotesByCode.get(code.getCode())))
                .filter(Objects::nonNull)
                .sorted(comparator(sortBy, ascending))
                .toList();
        int normalizedCount = Math.max(1, Math.min(count <= 0 ? 200 : count, 1000));
        return items.size() > normalizedCount ? items.subList(0, normalizedCount) : items;
    }

    @Override
    public IndustryApplicationBO.FundFlowSnapshot getFundFlow(LocalDate tradeDate, Integer top) {
        int normalizedTop = top == null ? 0 : Math.max(0, Math.min(top, FUND_FLOW_FULL_LIMIT));
        if (tradeDate == null || LocalDate.now().equals(tradeDate)) {
            IndustryApplicationBO.FundFlowSnapshot intradaySnapshot = getLatestPulseFundFlow(tradeDate, normalizedTop);
            if (intradaySnapshot != null) {
                return intradaySnapshot;
            }
        }
        return getDailyFundFlow(tradeDate, normalizedTop);
    }

    private IndustryApplicationBO.FundFlowSnapshot getDailyFundFlow(LocalDate tradeDate, int normalizedTop) {
        LocalDate resolvedTradeDate = sectorFundFlowDailySnapshotBasicService.findLatestTradeDate(FUND_FLOW_SCOPE, tradeDate);
        if (resolvedTradeDate == null) {
            return IndustryApplicationBO.FundFlowSnapshot.builder()
                    .ok(false)
                    .tradeDate(tradeDate)
                    .requestedTradeDate(tradeDate)
                    .fallbackTradeDate(false)
                    .servingMode("close")
                    .rowCount(0)
                    .totalPages(0)
                    .pageRowCounts(List.of())
                    .rows(List.of())
                    .source("sector_fund_flow_daily_snapshots")
                    .sourceKind("daily_snapshot")
                    .error("no industry fund-flow snapshot found")
                    .build();
        }

        List<SectorFundFlowDailySnapshotPO> allRows = sectorFundFlowDailySnapshotBasicService.listByTradeDate(
                FUND_FLOW_SCOPE,
                resolvedTradeDate,
                FUND_FLOW_FULL_LIMIT
        );
        List<SectorFundFlowDailySnapshotPO> visibleRows = normalizedTop > 0 && allRows.size() > normalizedTop
                ? allRows.subList(0, normalizedTop)
                : allRows;
        boolean stale = tradeDate != null && resolvedTradeDate.isBefore(tradeDate);
        return IndustryApplicationBO.FundFlowSnapshot.builder()
                .ok(!visibleRows.isEmpty())
                .tradeDate(resolvedTradeDate)
                .requestedTradeDate(tradeDate)
                .fallbackTradeDate(stale)
                .servingMode("close")
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
                .sourceKind("daily_snapshot")
                .stale(stale)
                .staleReason(stale ? "requested trade date has no snapshot; using latest previous snapshot" : null)
                .error(visibleRows.isEmpty() ? "no industry fund-flow rows found" : null)
                .build();
    }

    private IndustryApplicationBO.FundFlowSnapshot getLatestPulseFundFlow(LocalDate requestedTradeDate, int normalizedTop) {
        LocalDate effectiveRequestedTradeDate = requestedTradeDate == null ? LocalDate.now() : requestedTradeDate;
        LocalDate resolvedTradeDate = marketPulseQueryRepository.findLatestPulseTradeDate(effectiveRequestedTradeDate)
                .or(() -> marketPulseQueryRepository.findLatestPulseTradeDate(null))
                .orElse(null);
        if (resolvedTradeDate == null) {
            return null;
        }
        List<MarketPulseSectorDailySnapshotBO> allRows = marketPulseQueryRepository.listPulseRowsByTradeDate(resolvedTradeDate).stream()
                .sorted(Comparator
                        .comparing(MarketPulseSectorDailySnapshotBO::getMainNet, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketPulseSectorDailySnapshotBO::getChangePct, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketPulseSectorDailySnapshotBO::getSectorName, Comparator.nullsLast(String::compareTo)))
                .toList();
        if (allRows.isEmpty()) {
            return null;
        }
        List<MarketPulseSectorDailySnapshotBO> visibleRows = normalizedTop > 0 && allRows.size() > normalizedTop
                ? allRows.subList(0, normalizedTop)
                : allRows;
        MarketPulseCaptureBatchBO batch = marketPulseQueryRepository.findLatestPulseBatch(resolvedTradeDate).orElse(null);
        Map<String, String> codeByIndustry = latestFundFlowCodeByIndustry();
        return IndustryApplicationBO.FundFlowSnapshot.builder()
                .ok(true)
                .tradeDate(resolvedTradeDate)
                .requestedTradeDate(effectiveRequestedTradeDate)
                .fallbackTradeDate(!resolvedTradeDate.equals(effectiveRequestedTradeDate))
                .servingMode(resolvePulseServingMode(resolvedTradeDate, batch))
                .rowCount(visibleRows.size())
                .totalPages(1)
                .pageRowCounts(List.of(allRows.size()))
                .fetchedAt(batch == null ? null : batch.getFetchedAt())
                .rows(toPulseFundFlowRows(visibleRows, codeByIndustry))
                .source(batch == null ? "postgres.market_pulse" : batch.getSourceName())
                .sourceKind(batch == null ? null : batch.getSourceKind())
                .stale(false)
                .staleReason(null)
                .error(null)
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

    @Override
    public IndustryApplicationBO.TdxFundFlowSnapshot getTdxFundFlow(String targetType, String symbol, LocalDate tradeDate, int days) {
        String normalizedType = normalizeTargetType(targetType, symbol);
        String normalizedSymbol = normalizeSymbol(normalizedType, symbol);
        int normalizedDays = Math.max(1, Math.min(days <= 0 ? 30 : days, 252));
        IndustryApplicationBO.TdxFundFlowSnapshot snapshot =
                industryApplicationDataClient.tdxFundFlow(normalizedType, normalizedSymbol, tradeDate, normalizedDays);
        if (snapshot != null && snapshot.getCode6() != null && !snapshot.getCode6().isBlank()) {
            List<IndustryApplicationBO.TdxThsIndustryMapping> mappings = listThsMappings(snapshot.getCode6());
            snapshot.setThsMappings(mappings);
            snapshot.setConstituents(mergeConstituents(snapshot.getCode6(), mappings));
        }
        return snapshot;
    }

    private List<IndustryApplicationBO.TdxFundFlowConstituent> mergeConstituents(
            String tdxCode6,
            List<IndustryApplicationBO.TdxThsIndustryMapping> mappings
    ) {
        Map<String, ConstituentAccumulator> merged = new LinkedHashMap<>();
        for (IndustryApplicationBO.TdxThsIndustryMapping mapping : mappings == null ? List.<IndustryApplicationBO.TdxThsIndustryMapping>of() : mappings) {
            String thsCode = mapping.getThsCode();
            if (thsCode == null || thsCode.isBlank()) {
                continue;
            }
            for (IndustryApplicationBO.TdxFundFlowConstituent item : listThsConstituents(thsCode)) {
                mergeConstituent(merged, item);
            }
        }
        for (IndustryApplicationBO.TdxFundFlowConstituent item : listTdxConstituents(tdxCode6)) {
            mergeConstituent(merged, item);
        }
        return merged.values().stream()
                .map(ConstituentAccumulator::toValue)
                .sorted(Comparator
                        .comparing(IndustryApplicationBO.TdxFundFlowConstituent::getChangePct,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(IndustryApplicationBO.TdxFundFlowConstituent::getCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private void mergeConstituent(Map<String, ConstituentAccumulator> merged, IndustryApplicationBO.TdxFundFlowConstituent item) {
        if (item == null || item.getCode() == null || item.getCode().isBlank()) {
            return;
        }
        merged.computeIfAbsent(item.getCode(), ignored -> new ConstituentAccumulator(item.getCode())).merge(item);
    }

    private List<IndustryApplicationBO.TdxFundFlowConstituent> listTdxConstituents(String code6) {
        return tdxSectorQuoteDetailSnapshotRepository.findLatestByCode6(code6)
                .map(snapshot -> {
                    try {
                        JsonNode root = objectMapper.readTree(snapshot.getPayload());
                        JsonNode rows = root.path("sections").path("constituents");
                        if (!rows.isArray()) {
                            return List.<IndustryApplicationBO.TdxFundFlowConstituent>of();
                        }
                        List<IndustryApplicationBO.TdxFundFlowConstituent> items = new ArrayList<>();
                        for (JsonNode row : rows) {
                            String code = text(row, "code");
                            if (code == null || code.isBlank()) {
                                continue;
                            }
                            items.add(IndustryApplicationBO.TdxFundFlowConstituent.builder()
                                    .code(code)
                                    .name(text(row, "name"))
                                    .price(decimal(row, "last_price"))
                                    .changePct(decimal(row, "change_pct"))
                                    .sources(List.of("tdx"))
                                    .build());
                        }
                        return items;
                    } catch (Exception ignored) {
                        return List.<IndustryApplicationBO.TdxFundFlowConstituent>of();
                    }
                })
                .orElseGet(List::of);
    }

    private List<IndustryApplicationBO.TdxFundFlowConstituent> listThsConstituents(String industryCode) {
        return thsIndustryConstituentSnapshotRepository.listLatestByIndustryCode(industryCode).stream()
                .map(row -> IndustryApplicationBO.TdxFundFlowConstituent.builder()
                        .code(row.getStockCode())
                        .name(row.getStockName())
                        .price(row.getPrice())
                        .changePct(row.getChangePct())
                        .turnoverPct(row.getTurnoverPct())
                        .amountText(row.getAmountText())
                        .sources(List.of("ths"))
                        .build())
                .toList();
    }

    private IndustryApplicationBO.Target toTarget(IndustryApplicationTargetPO po) {
        String resolvedName = po.getName();
        if (resolvedName == null || resolvedName.isBlank() || resolvedName.equalsIgnoreCase(po.getSymbol())) {
            resolvedName = lookupName(po.getTargetType(), po.getSymbol());
        }
        return IndustryApplicationBO.Target.builder()
                .id(po.getId())
                .targetKey(po.getTargetKey())
                .targetType(po.getTargetType())
                .symbol(po.getSymbol())
                .name(resolvedName)
                .enabled(po.getEnabled())
                .intervalMinutes(po.getIntervalMinutes())
                .tags(readTags(po.getTags()))
                .displayOrder(po.getDisplayOrder())
                .build();
    }

    private IndustryApplicationTargetPO toTargetPO(IndustryApplicationBO.Target value, int order) {
        String targetType = normalizeTargetType(value.getTargetType(), value.getSymbol());
        String symbol = normalizeSymbol(targetType, value.getSymbol());
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
                                                                    ClickHouseStockChartClient.DailyQuote quote) {
        if (quote == null) {
            return null;
        }
        return IndustryApplicationBO.SectorOverviewItem.builder()
                .fullCode(code.getCode())
                .name(code.getName())
                .kind(code.getKind())
                .lastPrice(quote.getClose())
                .preClosePrice(quote.getPreClose())
                .openPrice(quote.getOpen())
                .highPrice(quote.getHigh())
                .lowPrice(quote.getLow())
                .change(quote.getChange())
                .changePct(quote.getChangePct())
                .amplitudePct(quote.getAmplitude())
                .highPct(percent(quote.getHigh().subtract(nullToZero(quote.getPreClose())), quote.getPreClose()))
                .lowPct(percent(quote.getLow().subtract(nullToZero(quote.getPreClose())), quote.getPreClose()))
                .openPct(percent(quote.getOpen().subtract(nullToZero(quote.getPreClose())), quote.getPreClose()))
                .volume(quote.getVolume() == null ? null : quote.getVolume().longValue())
                .amount(quote.getAmount())
                .tradingDate(quote.getTradeDate())
                .build();
    }

    private List<IndustryApplicationBO.Bar> loadBarsFromClickHouse(String normalizedSymbol, int normalizedCount) {
        ClickHouseStockChartClient.MarketSymbol marketSymbol = toMarketSymbol(normalizedSymbol);
        List<ClickHouseStockChartClient.DailyBar> rows = clickHouseStockChartClient.listDailyBars(
                marketSymbol.market(),
                marketSymbol.symbol(),
                null,
                null
        );
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<IndustryApplicationBO.Bar> bars = rows.stream()
                .map(this::toIndustryBar)
                .filter(bar -> bar.getTradeDate() != null)
                .sorted(Comparator.comparing(IndustryApplicationBO.Bar::getTradeDate))
                .collect(Collectors.toCollection(ArrayList::new));
        fillPreviousCloseAndPct(bars);
        if (bars.size() > normalizedCount) {
            return new ArrayList<>(bars.subList(bars.size() - normalizedCount, bars.size()));
        }
        return bars;
    }

    private IndustryApplicationBO.Bar toIndustryBar(ClickHouseStockChartClient.DailyBar row) {
        if (row == null || row.getTradeDate() == null) {
            return IndustryApplicationBO.Bar.builder().build();
        }
        return IndustryApplicationBO.Bar.builder()
                .tradeDate(row.getTradeDate())
                .time(row.getTradeDate() + " 15:00:00+08:00")
                .open(row.getOpen())
                .high(row.getHigh())
                .low(row.getLow())
                .close(row.getClose())
                .volumeLots(row.getVolume() == null ? null : row.getVolume().longValue())
                .amount(row.getAmount())
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

    private List<IndustryApplicationBO.FundFlowRow> toPulseFundFlowRows(List<MarketPulseSectorDailySnapshotBO> rows,
                                                                        Map<String, String> codeByIndustry) {
        List<IndustryApplicationBO.FundFlowRow> out = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            MarketPulseSectorDailySnapshotBO row = rows.get(index);
            out.add(IndustryApplicationBO.FundFlowRow.builder()
                    .rank(index + 1)
                    .industry(row.getSectorName())
                    .code(codeByIndustry.get(row.getSectorName()))
                    .changePct(row.getChangePct())
                    .inflow(row.getInflow())
                    .outflow(row.getOutflow())
                    .net(row.getMainNet())
                    .companyCount(row.getStockCount())
                    .leaderStock(row.getLeadingStock())
                    .leaderChange(row.getLeadingChangePct())
                    .leaderPrice(row.getLeadingPrice())
                    .tradeDate(row.getTradeDate())
                    .source(row.getSourceName())
                    .build());
        }
        return out;
    }

    private Map<String, String> latestFundFlowCodeByIndustry() {
        LocalDate latestTradeDate = sectorFundFlowDailySnapshotBasicService.findLatestTradeDate(FUND_FLOW_SCOPE, null);
        if (latestTradeDate == null) {
            return Map.of();
        }
        return sectorFundFlowDailySnapshotBasicService.listByTradeDate(FUND_FLOW_SCOPE, latestTradeDate, FUND_FLOW_FULL_LIMIT).stream()
                .filter(row -> row.getSectorName() != null && !row.getSectorName().isBlank())
                .filter(row -> row.getSectorCode() != null && !row.getSectorCode().isBlank())
                .collect(Collectors.toMap(
                        SectorFundFlowDailySnapshotPO::getSectorName,
                        SectorFundFlowDailySnapshotPO::getSectorCode,
                        (left, right) -> left
                ));
    }

    private String resolvePulseServingMode(LocalDate tradeDate, MarketPulseCaptureBatchBO batch) {
        if (tradeDate != null && tradeDate.equals(LocalDate.now()) && batch != null && "live_capture".equals(batch.getSourceKind())) {
            return "intraday";
        }
        return "close";
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

    private ClickHouseStockChartClient.MarketSymbol toMarketSymbol(String fullCode) {
        String normalized = normalizeSymbol("", fullCode);
        return new ClickHouseStockChartClient.MarketSymbol(
                normalized.substring(0, 2).toUpperCase(),
                normalized.substring(2)
        );
    }

    private String toFullCode(String market, String symbol) {
        return (market == null ? "" : market.trim().toLowerCase()) + (symbol == null ? "" : symbol.trim());
    }

    private String normalizeTargetType(String targetType, String symbol) {
        String normalizedSymbol = normalizeSymbol(targetType, symbol);
        Optional<String> catalogKind = lookupKindByCode(normalizedSymbol);
        if (catalogKind.isPresent()) {
            return catalogKind.get();
        }
        String value = targetType == null ? "" : targetType.trim().toLowerCase();
        if ("industry".equals(value) || "concept".equals(value) || "index".equals(value)) {
            return value;
        }
        if (normalizedSymbol.startsWith("sh8804")) {
            return "industry";
        }
        if (normalizedSymbol.startsWith("sh8805")) {
            return "concept";
        }
        if (normalizedSymbol.startsWith("sh8803")) {
            return "industry";
        }
        return "index";
    }

    private String normalizeSymbol(String targetType, String symbol) {
        String value = symbol == null ? "" : symbol.trim().toLowerCase();
        if (value.matches("^[a-z]{2}\\d{6}$")) {
            return value;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 6) {
            if ("index".equalsIgnoreCase(targetType) || lookupKindByCode("sh" + digits).filter("index"::equals).isPresent()) {
                return digits.startsWith("399") ? "sz" + digits : "sh" + digits;
            }
            if (digits.startsWith("880")) {
                return "sh" + digits;
            }
            if (digits.startsWith("399")) {
                return "sz" + digits;
            }
            if (digits.startsWith("6") || digits.startsWith("5") || digits.startsWith("9")) {
                return "sh" + digits;
            }
            if (digits.startsWith("8") || digits.startsWith("4")) {
                return "bj" + digits;
            }
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
        return industryApplicationTargetCatalogProvider.lookupName(symbol)
                .orElse(symbol);
    }

    private Optional<String> lookupKindByCode(String symbol) {
        return industryApplicationTargetCatalogProvider.lookupKind(symbol);
    }

    private List<IndustryApplicationBO.TargetCode> currentTargetCodes() {
        return industryApplicationTargetCatalogProvider.mergeAvailable(clickHouseStockChartClient.listAvailableIndustryApplicationSymbols());
    }

    private void fillPreviousCloseAndPct(List<IndustryApplicationBO.Bar> bars) {
        IndustryApplicationBO.Bar previous = null;
        for (IndustryApplicationBO.Bar bar : bars) {
            if (previous != null) {
                bar.setPrevClose(previous.getClose());
                if (previous.getClose() != null && previous.getClose().compareTo(BigDecimal.ZERO) != 0 && bar.getClose() != null) {
                    BigDecimal pct = bar.getClose()
                            .divide(previous.getClose(), 10, RoundingMode.HALF_UP)
                            .subtract(BigDecimal.ONE)
                            .multiply(BigDecimal.valueOf(100));
                    bar.setPct(pct);
                }
            }
            previous = bar;
        }
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

    private List<IndustryApplicationBO.TdxThsIndustryMapping> listThsMappings(String tdxCode6) {
        return tdxThsIndustryMappingBasicService.listByTdxCode6(tdxCode6).stream()
                .map(this::toTdxThsIndustryMapping)
                .toList();
    }

    private IndustryApplicationBO.TdxThsIndustryMapping toTdxThsIndustryMapping(TdxThsIndustryMappingPO po) {
        return IndustryApplicationBO.TdxThsIndustryMapping.builder()
                .tdxCode6(po.getTdxCode6())
                .tdxName(po.getTdxName())
                .thsCode(po.getThsCode())
                .thsName(po.getThsName())
                .matchType(po.getMatchType())
                .confidence(po.getConfidence())
                .priority(po.getPriority())
                .primary(po.getIsPrimary())
                .remark(po.getRemark())
                .source(po.getSource())
                .build();
    }

    private static String text(JsonNode node, String field) {
        if (node == null || field == null) {
            return null;
        }
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static final class ConstituentAccumulator {
        private final String code;
        private String name;
        private BigDecimal price;
        private BigDecimal changePct;
        private BigDecimal turnoverPct;
        private String amountText;
        private final LinkedHashSet<String> sources = new LinkedHashSet<>();

        private ConstituentAccumulator(String code) {
            this.code = code;
        }

        private void merge(IndustryApplicationBO.TdxFundFlowConstituent item) {
            if (item == null) {
                return;
            }
            if (this.name == null || this.name.isBlank()) {
                this.name = item.getName();
            }
            if (this.price == null) {
                this.price = item.getPrice();
            }
            if (this.changePct == null) {
                this.changePct = item.getChangePct();
            }
            if (this.turnoverPct == null) {
                this.turnoverPct = item.getTurnoverPct();
            }
            if (this.amountText == null || this.amountText.isBlank()) {
                this.amountText = item.getAmountText();
            }
            if (item.getSources() != null) {
                this.sources.addAll(item.getSources().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList());
            }
        }

        private IndustryApplicationBO.TdxFundFlowConstituent toValue() {
            return IndustryApplicationBO.TdxFundFlowConstituent.builder()
                    .code(code)
                    .name(name)
                    .price(price)
                    .changePct(changePct)
                    .turnoverPct(turnoverPct)
                    .amountText(amountText)
                    .sources(new ArrayList<>(sources))
                    .build();
        }
    }

}
