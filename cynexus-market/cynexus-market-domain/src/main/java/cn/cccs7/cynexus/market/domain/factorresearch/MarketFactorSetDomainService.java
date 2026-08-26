package cn.cccs7.cynexus.market.domain.factorresearch;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import cn.cccs7.cynexus.market.domain.service.screener.StockScreenerQueryDomainService;
import cn.cccs7.cynexus.market.domain.factorresearch.discovery.PromotedFactorRecipeEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketFactorSetDomainService {
    private final MarketFactorSetRepository repository;
    private final MarketFactorSetIdGenerator ids;
    private final MarketFactorExpressionPolicy expressionPolicy;
    private final StockScreenerQueryDomainService screener;
    private final MarketFactorStudyRepository studies;
    private final MarketFactorStudySnapshotRepository snapshots;
    private final PromotedFactorRecipeEvaluator recipeEvaluator;
    public MarketFactorSetDomainService(MarketFactorSetRepository repository, MarketFactorSetIdGenerator ids) { this(repository, ids, new MarketFactorExpressionPolicy(), null, null, null, new PromotedFactorRecipeEvaluator()); }
    public MarketFactorSetDomainService(MarketFactorSetRepository repository, MarketFactorSetIdGenerator ids, StockScreenerQueryDomainService screener) { this(repository, ids, new MarketFactorExpressionPolicy(), screener, null, null, new PromotedFactorRecipeEvaluator()); }
    @Autowired
    public MarketFactorSetDomainService(MarketFactorSetRepository repository, MarketFactorSetIdGenerator ids, StockScreenerQueryDomainService screener,
                                        MarketFactorStudyRepository studies, MarketFactorStudySnapshotRepository snapshots) {
        this(repository, ids, new MarketFactorExpressionPolicy(), screener, studies, snapshots, new PromotedFactorRecipeEvaluator());
    }
    MarketFactorSetDomainService(MarketFactorSetRepository repository, MarketFactorSetIdGenerator ids, MarketFactorExpressionPolicy expressionPolicy) { this(repository, ids, expressionPolicy, null, null, null, new PromotedFactorRecipeEvaluator()); }
    private MarketFactorSetDomainService(MarketFactorSetRepository repository, MarketFactorSetIdGenerator ids, MarketFactorExpressionPolicy expressionPolicy, StockScreenerQueryDomainService screener, MarketFactorStudyRepository studies, MarketFactorStudySnapshotRepository snapshots, PromotedFactorRecipeEvaluator recipeEvaluator) { this.repository = repository; this.ids = ids; this.expressionPolicy = expressionPolicy; this.screener = screener; this.studies = studies; this.snapshots = snapshots; this.recipeEvaluator = recipeEvaluator; }
    public List<MarketFactorSetBO.Definition> list(MarketFactorSetBO.Scope scope, int offset, int limit) { require(scope); return repository.list(scope.tenantId(), scope.ownerId(), Math.max(offset,0), Math.min(Math.max(limit,1),200)); }
    public MarketFactorSetBO.Definition get(MarketFactorSetBO.Scope scope, String code) { require(scope); return repository.find(scope.tenantId(), scope.ownerId(), normalize(code)).orElseThrow(() -> new IllegalArgumentException("factor set is unavailable")); }
    public MarketFactorSetBO.Definition create(MarketFactorSetBO.Scope scope, MarketFactorSetBO.CreateCommand cmd) {
        require(scope); if (cmd == null) throw new IllegalArgumentException("factor set request is required");
        String code = normalize(cmd.code()); String name = text(cmd.name(), "name", 128); Map<String,Object> expression = expr(cmd.expression());
        long id = ids.nextId(), versionId = ids.nextId();
        MarketFactorSetBO.Version v = new MarketFactorSetBO.Version(versionId, 1, cmd.sourceStudyId(), expression, Map.of(), MarketFactorSetBO.VersionStatus.DRAFT, null);
        MarketFactorSetBO.Definition d = new MarketFactorSetBO.Definition(id, code, scope.tenantId(), scope.ownerId(), name, MarketFactorSetBO.DefinitionStatus.DRAFT, null, 0, v);
        if (!repository.insertDefinition(d) || !repository.insertVersion(id, v)) throw new IllegalStateException("factor set could not be created");
        return d;
    }
    public MarketFactorSetBO.Definition update(MarketFactorSetBO.Scope scope, String code, MarketFactorSetBO.UpdateCommand cmd) {
        MarketFactorSetBO.Definition current = get(scope, code); if (cmd == null || cmd.expectedVersion() < 0) throw new IllegalArgumentException("expectedVersion is required");
        if (current.version() != cmd.expectedVersion()) throw new IllegalStateException("factor set was changed concurrently");
        String name = text(cmd.name(), "name", 128); Map<String,Object> expression = expr(cmd.expression());
        long vid = ids.nextId(); int no = current.activeVersion() == null ? 1 : current.activeVersion().versionNo() + 1;
        MarketFactorSetBO.Version v = new MarketFactorSetBO.Version(vid, no, cmd.sourceStudyId(), expression, Map.of(), MarketFactorSetBO.VersionStatus.DRAFT, null);
        MarketFactorSetBO.Definition next = new MarketFactorSetBO.Definition(current.id(), current.code(), current.tenantId(), current.ownerId(), name, current.status(), null, current.version()+1, v);
        if (!repository.updateDefinition(next, cmd.expectedVersion()) || !repository.insertVersion(current.id(), v)) throw new IllegalStateException("factor set was changed concurrently");
        return next;
    }
    public MarketFactorSetBO.Version validate(MarketFactorSetBO.Scope scope, String code, int versionNo, Map<String,Object> snapshot) {
        MarketFactorSetBO.Definition d = get(scope, code); if (snapshot == null) snapshot = Map.of();
        requireLatestDraft(d, versionNo);
        if (!repository.updateVersionStatus(d.id(), versionNo, MarketFactorSetBO.VersionStatus.VALIDATED, snapshot)) throw new IllegalArgumentException("factor set version is unavailable");
        MarketFactorSetBO.Version latest = d.activeVersion();
        return new MarketFactorSetBO.Version(latest.id(), versionNo, latest.sourceStudyId(), latest.expression(), snapshot, MarketFactorSetBO.VersionStatus.VALIDATED, latest.createdAt());
    }
    public MarketFactorSetBO.Version promote(MarketFactorSetBO.Scope scope, String code, int versionNo) {
        MarketFactorSetBO.Definition d = get(scope, code); if (d.activeVersion() == null || d.activeVersion().versionNo() != versionNo || d.activeVersion().status() != MarketFactorSetBO.VersionStatus.VALIDATED) throw new IllegalStateException("only validated factor set versions may be promoted");
        if (!repository.updateVersionStatus(d.id(), versionNo, MarketFactorSetBO.VersionStatus.PROMOTED, d.activeVersion().validationSnapshot())) throw new IllegalStateException("factor set promotion failed");
        if (!repository.activateVersion(d.id(), scope.tenantId(), scope.ownerId(), d.activeVersion().id())) throw new IllegalStateException("factor set activation failed");
        return new MarketFactorSetBO.Version(d.activeVersion().id(), d.activeVersion().versionNo(), d.activeVersion().sourceStudyId(), d.activeVersion().expression(), d.activeVersion().validationSnapshot(), MarketFactorSetBO.VersionStatus.PROMOTED, d.activeVersion().createdAt());
    }
    /** A strategy handoff is evidence only: it never creates or mutates a strategy. */
    public MarketFactorSetBO.Version requirePromotedForStrategy(MarketFactorSetBO.Scope scope, String code, int versionNo) {
        MarketFactorSetBO.Definition definition = get(scope, code);
        MarketFactorSetBO.Version version = definition.activeVersion();
        if (version == null || version.versionNo() != versionNo || version.status() != MarketFactorSetBO.VersionStatus.PROMOTED) {
            throw new IllegalStateException("only a promoted factor set version may enter strategy handoff");
        }
        return version;
    }
    public MarketFactorSetBO.Preview preview(MarketFactorSetBO.Scope scope, String code, int versionNo, LocalDate tradeDate, int limit) {
        MarketFactorSetBO.Version version = requirePromotedForStrategy(scope, code, versionNo);
        if (screener == null) throw new IllegalStateException("screen preview runtime is unavailable");
        PreviewQuery query = previewQuery(version.expression(), tradeDate, limit);
        if (query.unsupported() != null) return new MarketFactorSetBO.Preview(version.id(), normalize(code), versionNo, tradeDate, null, "UNAVAILABLE", 0, List.of(), List.of(query.unsupported()));
        StockScreenerQueryBO.Result result = screener.screen(query.input());
        List<StockScreenerQueryBO.Row> screened = result.items();
        List<String> warnings = new ArrayList<>(result.warnings() == null ? List.of() : result.warnings());
        if (!query.recipes().isEmpty()) {
            if (studies == null || snapshots == null) return new MarketFactorSetBO.Preview(version.id(), normalize(code), versionNo, tradeDate, result.tradeDate(), "UNAVAILABLE", 0, List.of(), List.of("promoted recipe runtime is unavailable"));
            screened = applyPromotedRecipes(scope, version, tradeDate, screened, query.recipes(), warnings);
        }
        List<MarketFactorSetBO.PreviewItem> items = screened.stream().limit(Math.min(200, Math.max(1, limit))).map(row -> new MarketFactorSetBO.PreviewItem(row.code(), row.name(), row.market(), row.industry(), row.price(), row.changePct(), query.recipes().isEmpty() ? "typed Factor Set expression" : "typed expression and promoted discovered recipe")).toList();
        String availability = !result.ok() ? "NO_DATA" : warnings.stream().anyMatch(value -> value.startsWith("missing point-in-time")) ? "PARTIAL" : "AVAILABLE";
        return new MarketFactorSetBO.Preview(version.id(), normalize(code), versionNo, tradeDate, result.tradeDate(), availability, screened.size(), items,
                warnings);
    }
    /** Executes the exact promoted Factor Set evaluator for strategy/backtest callers. */
    public RecipeScreenResult screenPromotedRows(MarketFactorSetBO.Scope scope, long versionId, LocalDate tradeDate, int limit) {
        MarketFactorSetBO.Definition definition = repository.findByVersionId(scope.tenantId(), scope.ownerId(), versionId)
                .orElseThrow(() -> new IllegalArgumentException("factor set version is unavailable"));
        MarketFactorSetBO.Version version = requirePromotedForStrategy(scope, definition.code(), definition.activeVersion().versionNo());
        PreviewQuery query = previewQuery(version.expression(), tradeDate, limit);
        if (query.unsupported() != null) throw new IllegalStateException(query.unsupported());
        StockScreenerQueryBO.Result result = screener.screen(query.input());
        List<String> warnings = new ArrayList<>(result.warnings() == null ? List.of() : result.warnings());
        List<StockScreenerQueryBO.Row> rows = query.recipes().isEmpty() ? result.items() : applyPromotedRecipes(scope, version, tradeDate, result.items(), query.recipes(), warnings);
        return new RecipeScreenResult(result, rows.stream().limit(Math.min(200, Math.max(1, limit))).toList(), List.copyOf(warnings));
    }
    private void require(MarketFactorSetBO.Scope s) { if (s == null || blank(s.tenantId()) || blank(s.ownerId())) throw new IllegalArgumentException("identity scope is required"); }
    private String normalize(String s) { if (blank(s) || s.trim().length()>64) throw new IllegalArgumentException("code is required and must be at most 64 characters"); return s.trim().toUpperCase(Locale.ROOT); }
    private String text(String s,String f,int n) { if (blank(s)||s.trim().length()>n) throw new IllegalArgumentException(f+" is required and must be at most "+n+" characters"); return s.trim(); }
    private void requireLatestDraft(MarketFactorSetBO.Definition definition, int versionNo) {
        MarketFactorSetBO.Version latest = definition.activeVersion();
        if (latest == null || latest.versionNo() != versionNo || latest.status() != MarketFactorSetBO.VersionStatus.DRAFT) {
            throw new IllegalStateException("only the latest draft Factor Set version may be validated");
        }
    }
    private Map<String,Object> expr(Map<String,Object> e) { expressionPolicy.validate(e); return Map.copyOf(e); }
    @SuppressWarnings("unchecked")
    private PreviewQuery previewQuery(Map<String,Object> expression, LocalDate tradeDate, int limit) {
        if (!"AND".equals(String.valueOf(expression.get("operator")))) return new PreviewQuery(null, List.of(), "OR expressions require a dedicated factor evaluator");
        BigDecimal minPrice = null, maxPrice = null, minChange = null, maxChange = null, minAmount = null, maxAmount = null, minTurnover = null, maxTurnover = null, minVolume = null, maxVolume = null, minPe = null, maxPe = null;
        String industry = null, universe = "ALL";
        List<?> clauses = (List<?>) expression.get("clauses"); List<Map<String,Object>> recipes = new ArrayList<>();
        for (Object raw : clauses) {
            Map<String,Object> clause = (Map<String,Object>) raw; String field = String.valueOf(clause.get("field")); String operator = String.valueOf(clause.get("operator"));
            if ("discoveryCode".equals(field) || "RECIPE_MATCH".equals(operator)) { recipes.add(clause); continue; }
            String value = String.valueOf(clause.get("value"));
            if ("industry".equals(field) && "EQUALS".equals(operator)) { industry = value; continue; }
            if ("market".equals(field) && "EQUALS".equals(operator)) { universe = value; continue; }
            BigDecimal lower = "BETWEEN".equals(operator) ? decimal(clause.get("lower")) : decimal(clause.get("value"));
            BigDecimal upper = "BETWEEN".equals(operator) ? decimal(clause.get("upper")) : decimal(clause.get("value"));
            boolean min = "GTE".equals(operator) || "BETWEEN".equals(operator) || "EQUALS".equals(operator);
            boolean max = "LTE".equals(operator) || "BETWEEN".equals(operator) || "EQUALS".equals(operator);
            switch (field) {
                case "price" -> { if (min) minPrice = lower; if (max) maxPrice = upper; }
                case "changePct" -> { if (min) minChange = lower; if (max) maxChange = upper; }
                case "amount" -> { if (min) minAmount = lower; if (max) maxAmount = upper; }
                case "turnoverPct" -> { if (min) minTurnover = lower; if (max) maxTurnover = upper; }
                case "volumeRatio" -> { if (min) minVolume = lower; if (max) maxVolume = upper; }
                case "pe" -> { if (min) minPe = lower; if (max) maxPe = upper; }
                default -> { return new PreviewQuery(null, List.of(), "unsupported preview field: " + field); }
            }
        }
        return new PreviewQuery(new StockScreenerQueryBO.QueryInput(tradeDate == null ? null : tradeDate.toString(), universe, true, true, true,
                minPrice, maxPrice, minChange, maxChange, minAmount, null, null, industry, null, null, null, null, "ALL", null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, minTurnover, maxTurnover, minVolume, maxVolume,
                minPe, maxPe, "amount", "DESC", 0, 200, null, null, null), List.copyOf(recipes), null);
    }
    private List<StockScreenerQueryBO.Row> applyPromotedRecipes(MarketFactorSetBO.Scope scope, MarketFactorSetBO.Version version, LocalDate tradeDate,
                                                                   List<StockScreenerQueryBO.Row> candidates, List<Map<String,Object>> clauses, List<String> warnings) {
        if (version.sourceStudyId() == null) throw new IllegalStateException("Factor Set sourceStudyId is required for discovered recipes");
        MarketFactorResearchBO.Study study = studies.findById(scope.tenantId(), scope.ownerId(), String.valueOf(version.sourceStudyId()))
                .orElseThrow(() -> new IllegalStateException("Factor Set source study is unavailable"));
        List<MarketFactorResearchBO.FactorResult> factors = clauses.stream().map(clause -> {
            String discoveryCode = String.valueOf(clause.get("value"));
            return studies.findPromotedFactorResult(scope.tenantId(), scope.ownerId(), study.id(), discoveryCode)
                    .orElseThrow(() -> new IllegalStateException("only promoted discovered factors may be screened: " + discoveryCode));
        }).toList();
        OffsetDateTime cutoff = tradeDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        List<StockScreenerQueryBO.Row> matched = new ArrayList<>();
        for (StockScreenerQueryBO.Row candidate : candidates) {
            List<MarketFactorStudySnapshotRepository.DailyPrimitivePoint> points = snapshots.findDailyPrimitives(candidate.code(), candidate.market(), tradeDate,
                    Math.min(252, Math.max(20, study.config().dailyPreWindowSessions())), cutoff, study.featureRegistryVersion());
            Map<String,List<Double>> primitives = primitiveSeries(points);
            Map<String,String> labels = new LinkedHashMap<>(); labels.put("industry", candidate.industry()); labels.put("market", candidate.market());
            boolean all = factors.stream().allMatch(factor -> recipeEvaluator.matches(factor.recipe(), factor.learnedBounds(), primitives, labels));
            if (all) matched.add(candidate); else if (points.isEmpty()) warnings.add("missing point-in-time primitive sequence for " + candidate.code());
        }
        return matched;
    }
    private Map<String,List<Double>> primitiveSeries(List<MarketFactorStudySnapshotRepository.DailyPrimitivePoint> points) {
        Map<String,List<Double>> result = new LinkedHashMap<>();
        for (MarketFactorStudySnapshotRepository.DailyPrimitivePoint point : points) for (Map.Entry<String,BigDecimal> value : point.values().entrySet()) {
            result.computeIfAbsent(value.getKey(), ignored -> new ArrayList<>()).add(value.getValue() == null ? null : value.getValue().doubleValue());
        }
        return result;
    }
    private BigDecimal decimal(Object value) { try { return new BigDecimal(String.valueOf(value)); } catch (RuntimeException e) { throw new IllegalArgumentException("preview numeric condition is invalid"); } }
    private record PreviewQuery(StockScreenerQueryBO.QueryInput input, List<Map<String,Object>> recipes, String unsupported) { }
    public record RecipeScreenResult(StockScreenerQueryBO.Result result, List<StockScreenerQueryBO.Row> rows, List<String> warnings) { }
    private boolean blank(String s) { return s == null || s.isBlank(); }
}
