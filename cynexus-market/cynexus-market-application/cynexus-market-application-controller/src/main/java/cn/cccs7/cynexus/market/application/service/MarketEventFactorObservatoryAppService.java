package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.MarketEventFactorObservatoryDTO;
import cn.cccs7.cynexus.market.domain.entity.factor.MarketEventFactorObservatoryBO;
import cn.cccs7.cynexus.market.domain.service.factor.MarketEventFactorObservatoryDomainService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class MarketEventFactorObservatoryAppService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final int CACHE_MAX_ENTRIES = 128;
    private final MarketEventFactorObservatoryDomainService domainService;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public MarketEventFactorObservatoryAppService(MarketEventFactorObservatoryDomainService domainService) {
        this.domainService = domainService;
    }

    public MarketEventFactorObservatoryDTO observe(String tradeDate, String cohort, BigDecimal minLargeMovePct,
                                                     Integer limit, String symbol) {
        String cacheKey = String.join("|", String.valueOf(tradeDate), String.valueOf(cohort),
                String.valueOf(minLargeMovePct), String.valueOf(limit), String.valueOf(symbol));
        CachedResult cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.value();
        if (cache.size() >= CACHE_MAX_ENTRIES) cache.entrySet().stream().min(Map.Entry.comparingByValue((left, right) -> left.expiresAt().compareTo(right.expiresAt())))
                .map(Map.Entry::getKey).ifPresent(cache::remove);
        MarketEventFactorObservatoryBO.Result result = domainService.observe(
                new MarketEventFactorObservatoryBO.Query(tradeDate, cohort, minLargeMovePct, limit, symbol));
        MarketEventFactorObservatoryDTO value = MarketEventFactorObservatoryDTO.builder().ok(result.ok()).requestedTradeDate(result.requestedTradeDate())
                .tradeDate(result.tradeDate()).cohort(result.cohort()).minLargeMovePct(result.minLargeMovePct())
                .populationCount(result.populationCount()).sampledCount(result.sampledCount())
                .marketFactors(result.marketFactors().stream().map(this::factor).toList())
                .events(result.events().stream().map(this::event).toList())
                .screeningHintsDeprecated(true)
                .screeningHints(result.screeningHints().stream().map(item -> MarketEventFactorObservatoryDTO.ScreeningHint.builder()
                        .criterion(item.criterion()).operator(item.operator()).value(item.value()).label(item.label()).basis(item.basis()).build()).toList())
                .warnings(result.warnings()).error(result.error()).build();
        cache.put(cacheKey, new CachedResult(value, Instant.now().plus(CACHE_TTL)));
        return value;
    }

    private record CachedResult(MarketEventFactorObservatoryDTO value, Instant expiresAt) { }

    private MarketEventFactorObservatoryDTO.Event event(MarketEventFactorObservatoryBO.Event value) {
        return MarketEventFactorObservatoryDTO.Event.builder().code(value.code()).name(value.name()).eventType(value.eventType())
                .changePct(value.changePct()).streak(value.streak()).thsIndustryCode(value.thsIndustryCode()).thsIndustryName(value.thsIndustryName())
                .tdxIndustryCode(value.tdxIndustryCode()).tdxIndustryName(value.tdxIndustryName()).concepts(value.concepts()).styles(value.styles())
                .factors(value.factors().stream().map(this::factor).toList()).build();
    }

    private MarketEventFactorObservatoryDTO.FactorFamily factor(MarketEventFactorObservatoryBO.FactorFamily value) {
        return MarketEventFactorObservatoryDTO.FactorFamily.builder().stage(value.stage()).code(value.code()).label(value.label())
                .status(value.status()).asOfDate(value.asOfDate()).source(value.source()).supportingEvidence(value.supportingEvidence())
                .counterEvidence(value.counterEvidence()).warnings(value.warnings()).metrics(value.metrics().stream()
                        .map(item -> MarketEventFactorObservatoryDTO.Metric.builder().key(item.key()).label(item.label()).value(item.value()).unit(item.unit()).build()).toList()).build();
    }
}
