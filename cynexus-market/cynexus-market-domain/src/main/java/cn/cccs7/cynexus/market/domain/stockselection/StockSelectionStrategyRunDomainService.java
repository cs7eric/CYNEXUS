package cn.cccs7.cynexus.market.domain.stockselection;

import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import cn.cccs7.cynexus.market.domain.service.screener.StockScreenerQueryDomainService;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorSetBO;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorSetDomainService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class StockSelectionStrategyRunDomainService {

    private static final int MAX_STORED_CANDIDATES = 100;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StockSelectionStrategyCatalogDomainService catalogService;
    private final StockScreenerQueryDomainService screenerService;
    private final StockSelectionStrategyRunRepository runRepository;
    private final StockSelectionIdGenerator idGenerator;
    private final MarketFactorSetDomainService factorSetService;

    public StockSelectionStrategyRunDomainService(StockSelectionStrategyCatalogDomainService catalogService,
                                                  StockScreenerQueryDomainService screenerService,
                                                  StockSelectionStrategyRunRepository runRepository,
                                                  StockSelectionIdGenerator idGenerator) {
        this(catalogService, screenerService, runRepository, idGenerator, null);
    }

    @Autowired
    public StockSelectionStrategyRunDomainService(StockSelectionStrategyCatalogDomainService catalogService,
                                                  StockScreenerQueryDomainService screenerService,
                                                  StockSelectionStrategyRunRepository runRepository,
                                                  StockSelectionIdGenerator idGenerator,
                                                  MarketFactorSetDomainService factorSetService) {
        this.catalogService = catalogService;
        this.screenerService = screenerService;
        this.runRepository = runRepository;
        this.idGenerator = idGenerator;
        this.factorSetService = factorSetService;
    }

    public StockSelectionStrategyRunBO.Run run(StockSelectionStrategyRunBO.Scope scope,
                                               String strategyCode,
                                               String tradeDate) {
        Evaluation evaluation = evaluate(scope, strategyCode, tradeDate, MAX_STORED_CANDIDATES);
        StockSelectionStrategyCatalogBO.Strategy strategy = evaluation.strategy();
        Map<String, Object> parameters = evaluation.parameters();
        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        StockScreenerQueryBO.Result result = evaluation.result();
        List<String> warnings = new ArrayList<>(evaluation.warnings());
        List<StockScreenerQueryBO.Row> rows = evaluation.rows();
        if (result.count() > rows.size()) {
            warnings.add("Run evidence stores the top " + rows.size() + " candidates; matched count remains " + result.count() + ".");
        }
        List<StockSelectionStrategyRunBO.Candidate> candidates = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            candidates.add(candidate(idGenerator.nextId(), rows.get(index), index + 1, strategy.code(), parameters));
        }
        OffsetDateTime completedAt = OffsetDateTime.now(ZoneOffset.UTC);
        String status = result.ok() ? candidates.isEmpty() ? "NO_DATA" : "SUCCEEDED" : "NO_DATA";
        StockSelectionStrategyRunBO.Run run = new StockSelectionStrategyRunBO.Run(idGenerator.nextId(), scope,
                strategy.code(), strategy.activeVersion().id(), strategy.activeVersion().versionNo(), result.requestedTradeDate(),
                result.tradeDate(), status, parameters, result.source(), result.universeCount(), result.count(), List.copyOf(warnings),
                startedAt, completedAt, List.copyOf(candidates));
        runRepository.save(run);
        return run;
    }

    /**
     * Replays one governed strategy against the screener snapshot available on or before the requested date.
     * The returned rows are deliberately not persisted; callers that need an audit record own their aggregate.
     */
    public Evaluation evaluate(String strategyCode, String tradeDate, int maxCandidates) {
        return evaluate(null, strategyCode, tradeDate, maxCandidates);
    }

    public Evaluation evaluate(StockSelectionStrategyRunBO.Scope scope, String strategyCode, String tradeDate, int maxCandidates) {
        if (maxCandidates < 1 || maxCandidates > MAX_STORED_CANDIDATES) {
            throw new IllegalArgumentException("maxCandidates must be between 1 and " + MAX_STORED_CANDIDATES);
        }
        StockSelectionStrategyCatalogBO.Strategy strategy = findStrategy(scope, strategyCode);
        Map<String, Object> parameters = parameters(strategy.activeVersion().parameterSchema());
        if (strategy.activeVersion().sourceFactorSetVersionId() != null) {
            if (factorSetService == null) throw new IllegalStateException("factor-set strategy evaluator is unavailable");
            if (scope == null) throw new IllegalArgumentException("factor-set strategy evaluation requires identity scope");
            MarketFactorSetDomainService.RecipeScreenResult factorResult = factorSetService.screenPromotedRows(
                    new MarketFactorSetBO.Scope(scope.tenantId(), scope.ownerId()), strategy.activeVersion().sourceFactorSetVersionId(),
                    LocalDate.parse(tradeDate), maxCandidates);
            List<StockScreenerQueryBO.Row> rows = factorResult.rows().stream().limit(maxCandidates).toList();
            return new Evaluation(strategy, parameters, factorResult.result(), rows, factorResult.warnings());
        }
        StockScreenerQueryBO.Result result = screenerService.screen(query(tradeDate, parameters, null, maxCandidates));
        List<String> warnings = new ArrayList<>(result.warnings());
        List<StockScreenerQueryBO.Row> rows = result.items().stream()
                .filter(item -> !requiresTdxIndustry(parameters) || item.tdxIndustryCode() != null)
                .limit(maxCandidates).toList();
        return new Evaluation(strategy, parameters, result, List.copyOf(rows), List.copyOf(warnings));
    }

    /** Evaluates one exact symbol against the same governed strategy conditions as a full-universe run. */
    public Evaluation evaluateForSymbol(String strategyCode, String tradeDate, StockSelectionBacktestBO.MarketSymbol symbol) {
        return evaluateForSymbol(null, strategyCode, tradeDate, symbol);
    }

    public Evaluation evaluateForSymbol(StockSelectionStrategyRunBO.Scope scope, String strategyCode, String tradeDate,
                                        StockSelectionBacktestBO.MarketSymbol symbol) {
        if (symbol == null) throw new IllegalArgumentException("symbol is required");
        StockSelectionStrategyCatalogBO.Strategy strategy = findStrategy(scope, strategyCode);
        Map<String, Object> parameters = parameters(strategy.activeVersion().parameterSchema());
        if (strategy.activeVersion().sourceFactorSetVersionId() != null) {
            if (factorSetService == null) throw new IllegalStateException("factor-set strategy evaluator is unavailable");
            if (scope == null) throw new IllegalArgumentException("factor-set strategy evaluation requires identity scope");
            MarketFactorSetDomainService.RecipeScreenResult factorResult = factorSetService.screenPromotedRows(
                    new MarketFactorSetBO.Scope(scope.tenantId(), scope.ownerId()), strategy.activeVersion().sourceFactorSetVersionId(),
                    LocalDate.parse(tradeDate), 200);
            List<StockScreenerQueryBO.Row> rows = factorResult.rows().stream().filter(row -> row.code().equals(symbol.code()) && row.market().equals(symbol.market())).limit(1).toList();
            return new Evaluation(strategy, parameters, factorResult.result(), rows, factorResult.warnings());
        }
        StockScreenerQueryBO.Result result = screenerService.screen(query(tradeDate, parameters, symbol, 1));
        List<String> warnings = new ArrayList<>(result.warnings());
        List<StockScreenerQueryBO.Row> rows = result.items().stream()
                .filter(item -> !requiresTdxIndustry(parameters) || item.tdxIndustryCode() != null)
                .limit(1).toList();
        return new Evaluation(strategy, parameters, result, List.copyOf(rows), List.copyOf(warnings));
    }

    private StockScreenerQueryBO.QueryInput query(String tradeDate,
                                                  Map<String, Object> values,
                                                  StockSelectionBacktestBO.MarketSymbol symbol,
                                                  int pageSize) {
        return new StockScreenerQueryBO.QueryInput(tradeDate, text(values, "universe", "ALL"), bool(values, "excludeSt", true),
                bool(values, "excludeChiNext", true), bool(values, "excludeStar", true),
                decimal(values, "minPrice"), decimal(values, "maxPrice"), decimal(values, "minChangePct"), decimal(values, "maxChangePct"),
                decimal(values, "minAmount"), decimal(values, "minMarketCap"), decimal(values, "maxMarketCap"), textOrNull(values, "industry"),
                textOrNull(values, "tdxIndustryCode"), textOrNull(values, "thsIndustryCode"), textOrNull(values, "conceptSectorId"), textOrNull(values, "styleSectorId"), textOrNull(values, "board"),
                decimal(values, "minAmplitudePct"), decimal(values, "maxAmplitudePct"), decimal(values, "minReturnPct5d"), decimal(values, "maxReturnPct5d"),
                decimal(values, "minReturnPct10d"), decimal(values, "maxReturnPct10d"), decimal(values, "minReturnPct30d"), decimal(values, "maxReturnPct30d"),
                decimal(values, "minReturnPct60d"), decimal(values, "maxReturnPct60d"), decimal(values, "minReturnPct252d"), decimal(values, "maxReturnPct252d"),
                decimal(values, "minMainNet"), decimal(values, "minMainNet5d"), decimal(values, "minTurnoverPct"), decimal(values, "maxTurnoverPct"),
                decimal(values, "minVolumeRatio"), decimal(values, "maxVolumeRatio"), decimal(values, "minPe"), decimal(values, "maxPe"),
                text(values, "sort", "amount"), text(values, "sortDir", "desc"), 1, pageSize,
                symbol == null ? null : symbol.code(), symbol == null ? null : symbol.market(), pattern(values));
    }

    private StockSelectionStrategyRunBO.Candidate candidate(long id, StockScreenerQueryBO.Row row, int rank,
                                                             String strategyCode, Map<String, Object> parameters) {
        List<StockSelectionStrategyRunBO.Evidence> evidence = evidence(row, strategyCode, parameters);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("market", row.market()); snapshot.put("price", row.price()); snapshot.put("changePct", row.changePct());
        snapshot.put("returnPct5d", row.returnPct5d()); snapshot.put("returnPct10d", row.returnPct10d()); snapshot.put("returnPct30d", row.returnPct30d());
        snapshot.put("amount", row.amount()); snapshot.put("mainNet", row.mainNet()); snapshot.put("turnoverPct", row.turnoverPct());
        snapshot.put("volumeRatio", row.volumeRatio()); snapshot.put("tdxIndustryCode", row.tdxIndustryCode()); snapshot.put("tdxIndustryName", row.tdxIndustryName());
        return new StockSelectionStrategyRunBO.Candidate(id, row.code(), row.name(), rank, score(row, strategyCode),
                evidence.stream().filter(StockSelectionStrategyRunBO.Evidence::matched).map(StockSelectionStrategyRunBO.Evidence::label).reduce((left, right) -> left + " · " + right).orElse("Strategy conditions matched"),
                java.util.Collections.unmodifiableMap(new LinkedHashMap<>(snapshot)), List.copyOf(evidence));
    }

    private List<StockSelectionStrategyRunBO.Evidence> evidence(StockScreenerQueryBO.Row row, String strategyCode, Map<String, Object> parameters) {
        List<StockSelectionStrategyRunBO.Evidence> items = new ArrayList<>();
        if (strategyCode != null && strategyCode.startsWith("USER_")) {
            return userEvidence(row, parameters);
        }
        if ("TREND_MOMENTUM".equals(strategyCode)) {
            items.add(threshold("returnPct10d", "10D return", decimal(parameters, "minReturnPct10d"), row.returnPct10d()));
            items.add(threshold("returnPct30d", "30D return", decimal(parameters, "minReturnPct30d"), row.returnPct30d()));
        } else if ("VOLUME_CAPITAL_RESONANCE".equals(strategyCode)) {
            items.add(threshold("amount", "Turnover", decimal(parameters, "minAmount"), row.amount()));
            items.add(threshold("mainNet", "Main net flow", decimal(parameters, "minMainNet"), row.mainNet()));
        } else {
            items.add(new StockSelectionStrategyRunBO.Evidence(idGenerator.nextId(), "tdxIndustry", "TDX industry membership", "PRESENT", "present", row.tdxIndustryName(), row.tdxIndustryCode() != null));
            items.add(threshold("returnPct10d", "10D return", decimal(parameters, "minReturnPct10d"), row.returnPct10d()));
        }
        items.add(threshold("turnoverPct", "Turnover rate", decimal(parameters, "minTurnoverPct"), row.turnoverPct()));
        items.add(threshold("volumeRatio", "Volume ratio", decimal(parameters, "minVolumeRatio"), row.volumeRatio()));
        return items;
    }

    private StockSelectionStrategyRunBO.Evidence threshold(String code, String label, BigDecimal expected, BigDecimal actual) {
        return new StockSelectionStrategyRunBO.Evidence(idGenerator.nextId(), code, label, "GTE", expected, actual,
                expected == null || actual != null && actual.compareTo(expected) >= 0);
    }

    public BigDecimal score(StockScreenerQueryBO.Row row, String strategyCode) {
        BigDecimal primary = switch (strategyCode) {
            case "VOLUME_CAPITAL_RESONANCE" -> scaled(row.mainNet(), new BigDecimal("100000000"), 60).add(scaled(row.amount(), new BigDecimal("1000000000"), 25));
            case "SECTOR_BACKED_MOMENTUM" -> scaled(row.returnPct10d(), new BigDecimal("10"), 55).add(scaled(row.returnPct5d(), new BigDecimal("5"), 25));
            default -> scaled(row.returnPct30d(), new BigDecimal("20"), 55).add(scaled(row.returnPct10d(), new BigDecimal("10"), 25));
        };
        return primary.add(scaled(row.volumeRatio(), new BigDecimal("2"), 10)).add(scaled(row.turnoverPct(), new BigDecimal("5"), 10))
                .min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaled(BigDecimal value, BigDecimal denominator, int weight) {
        if (value == null || value.signum() <= 0) return BigDecimal.ZERO;
        return value.divide(denominator, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(weight)).min(BigDecimal.valueOf(weight));
    }

    private List<StockSelectionStrategyRunBO.Evidence> userEvidence(StockScreenerQueryBO.Row row, Map<String, Object> parameters) {
        List<StockSelectionStrategyRunBO.Evidence> evidence = new ArrayList<>();
        parameters.forEach((key, expected) -> {
            if (!Set.of("universe", "sort", "sortDir").contains(key)) evidence.add(new StockSelectionStrategyRunBO.Evidence(
                    idGenerator.nextId(), key, label(key), operator(key), expected, actual(row, key), true));
        });
        return evidence;
    }

    private Object actual(StockScreenerQueryBO.Row row, String key) {
        return switch (key) {
            case "excludeSt" -> !row.st(); case "excludeChiNext" -> !"chinext".equals(row.board()); case "excludeStar" -> !"star".equals(row.board()); case "industry" -> row.industry(); case "tdxIndustryCode" -> row.tdxIndustryCode();
            case "thsIndustryCode" -> row.thsIndustryCode(); case "board" -> row.board(); case "minPrice", "maxPrice" -> row.price();
            case "minChangePct", "maxChangePct" -> row.changePct(); case "minAmount" -> row.amount();
            case "minMarketCap", "maxMarketCap" -> row.totalMarketCap(); case "minAmplitudePct", "maxAmplitudePct" -> row.amplitudePct();
            case "minReturnPct5d", "maxReturnPct5d" -> row.returnPct5d(); case "minReturnPct10d", "maxReturnPct10d" -> row.returnPct10d();
            case "minReturnPct30d", "maxReturnPct30d" -> row.returnPct30d(); case "minReturnPct60d", "maxReturnPct60d" -> row.returnPct60d();
            case "minReturnPct252d", "maxReturnPct252d" -> row.returnPct252d(); case "minMainNet" -> row.mainNet();
            case "minMainNet5d" -> row.mainNet5d(); case "minTurnoverPct", "maxTurnoverPct" -> row.turnoverPct();
            case "minVolumeRatio", "maxVolumeRatio" -> row.volumeRatio(); case "minPe", "maxPe" -> row.pe();
            case "volumeContractionDays" -> row.priceVolumePattern() == null ? null : row.priceVolumePattern().observedContractionDays();
            case "minBreakoutVolumeMultiple", "maxBreakoutVolumeMultiple" -> row.priceVolumePattern() == null ? null : row.priceVolumePattern().breakoutVolumeMultiple();
            case "minBullishBodyPct" -> row.priceVolumePattern() == null ? null : row.priceVolumePattern().bullishBodyPct();
            case "minCloseAboveMa20Pct" -> row.priceVolumePattern() == null ? null : row.priceVolumePattern().closeAboveMa20Pct();
            case "minMa5AboveMa20Pct" -> row.priceVolumePattern() == null ? null : row.priceVolumePattern().ma5AboveMa20Pct();
            case "breakoutHighDays" -> row.priceVolumePattern() == null ? null : row.priceVolumePattern().breakoutHighDays();
            case "maxAtrPct20" -> row.priceVolumePattern() == null ? null : row.priceVolumePattern().atrPct20();
            case "minConsecutiveBullishDays" -> row.priceVolumePattern() == null ? null : row.priceVolumePattern().consecutiveBullishDays();
            case "minVolumeVsMa5Multiple", "maxVolumeVsMa5Multiple" -> row.priceVolumePattern() == null ? null : row.priceVolumePattern().volumeVsMa5Multiple();
            case "candlestickPattern" -> row.priceVolumePattern() == null ? null : row.priceVolumePattern().candlestickPatterns();
            case "conceptSectorId" -> row.concepts(); case "styleSectorId" -> row.styles(); case "requiresTdxIndustry" -> row.tdxIndustryCode() != null;
            default -> null;
        };
    }

    private String label(String key) { return key.replaceAll("([A-Z])", " $1").trim(); }
    private String operator(String key) { return key.startsWith("max") ? "LTE" : key.startsWith("min") || "volumeContractionDays".equals(key) ? "GTE" : "MATCH"; }

    private Map<String, Object> parameters(String parameterSchema) {
        try {
            return Map.copyOf(OBJECT_MAPPER.readValue(parameterSchema, new TypeReference<Map<String, Object>>() { }));
        } catch (Exception exception) {
            throw new IllegalStateException("active strategy parameters are invalid", exception);
        }
    }

    private String normalizeCode(String value) { return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT); }
    private StockSelectionStrategyCatalogBO.Strategy findStrategy(StockSelectionStrategyRunBO.Scope scope, String strategyCode) {
        StockSelectionStrategyCatalogBO.Catalog catalog = scope == null ? catalogService.listActive() : catalogService.listActive(scope);
        return catalog.items().stream().filter(item -> item.code().equalsIgnoreCase(normalizeCode(strategyCode))).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("strategy is not active"));
    }
    private boolean requiresTdxIndustry(Map<String, Object> values) { return bool(values, "requiresTdxIndustry", false); }
    private String text(Map<String, Object> values, String key, String fallback) { Object value = values.get(key); return value == null ? fallback : String.valueOf(value); }
    private String textOrNull(Map<String, Object> values, String key) { Object value = values.get(key); return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value); }
    private boolean bool(Map<String, Object> values, String key, boolean fallback) { Object value = values.get(key); return value instanceof Boolean bool ? bool : value == null ? fallback : Boolean.parseBoolean(String.valueOf(value)); }
    private BigDecimal decimal(Map<String, Object> values, String key) { Object value = values.get(key); return value == null ? null : new BigDecimal(String.valueOf(value)); }
    private Integer integer(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).isBlank()) return null;
        return value instanceof Number number ? number.intValue() : Integer.valueOf(String.valueOf(value));
    }
    private StockScreenerQueryBO.PriceVolumePattern pattern(Map<String, Object> values) {
        Integer days = values.get("volumeContractionDays") == null ? null : Integer.valueOf(String.valueOf(values.get("volumeContractionDays")));
        return new StockScreenerQueryBO.PriceVolumePattern(days, decimal(values, "minBreakoutVolumeMultiple"), decimal(values, "maxBreakoutVolumeMultiple"), decimal(values, "minBullishBodyPct"),
                decimal(values, "minCloseAboveMa20Pct"), decimal(values, "minMa5AboveMa20Pct"), integer(values, "breakoutHighDays"),
                decimal(values, "maxAtrPct20"), integer(values, "minConsecutiveBullishDays"), decimal(values, "minVolumeVsMa5Multiple"),
                decimal(values, "maxVolumeVsMa5Multiple"), textOrNull(values, "candlestickPattern"));
    }

    public record Evaluation(StockSelectionStrategyCatalogBO.Strategy strategy,
                             Map<String, Object> parameters,
                             StockScreenerQueryBO.Result result,
                             List<StockScreenerQueryBO.Row> rows,
                             List<String> warnings) {
    }
}
