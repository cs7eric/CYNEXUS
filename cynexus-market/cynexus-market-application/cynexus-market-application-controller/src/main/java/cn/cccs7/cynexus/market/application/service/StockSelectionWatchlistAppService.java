package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.StockSelectionWatchlistDTO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionWatchlistBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionWatchlistDomainService;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionWatchlistObservationBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionWatchlistObservationDomainService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockSelectionWatchlistAppService {

    private final StockSelectionWatchlistDomainService domainService;
    private final StockSelectionWatchlistObservationDomainService observationDomainService;

    public StockSelectionWatchlistAppService(StockSelectionWatchlistDomainService domainService,
                                              StockSelectionWatchlistObservationDomainService observationDomainService) {
        this.domainService = domainService;
        this.observationDomainService = observationDomainService;
    }

    public StockSelectionWatchlistDTO.ListResponse list(StockSelectionStrategyRunBO.Scope scope) {
        return StockSelectionWatchlistDTO.ListResponse.builder()
                .items(domainService.list(scope).stream().map(this::item).toList())
                .build();
    }

    public StockSelectionWatchlistDTO.PerformanceResponse performance(StockSelectionStrategyRunBO.Scope scope) {
        StockSelectionWatchlistObservationBO.Result result = observationDomainService.observe(scope);
        return StockSelectionWatchlistDTO.PerformanceResponse.builder()
                .items(result.items().stream().map(this::performance).toList())
                .dataSource(result.dataSource())
                .warnings(result.warnings())
                .build();
    }

    public StockSelectionWatchlistDTO.PerformanceHistoryResponse performanceHistory(StockSelectionStrategyRunBO.Scope scope,
                                                                                       long itemId,
                                                                                       int limit) {
        StockSelectionWatchlistObservationBO.History result = observationDomainService.history(scope, itemId, limit);
        return StockSelectionWatchlistDTO.PerformanceHistoryResponse.builder()
                .watchlistItemId(result.watchlistItemId())
                .entryTradeDate(date(result.entryTradeDate()))
                .entryPrice(result.entryPrice())
                .points(result.points().stream().map(this::performancePoint).toList())
                .dataSource(result.dataSource())
                .warnings(result.warnings())
                .build();
    }

    @Transactional
    public StockSelectionWatchlistDTO.Item add(StockSelectionStrategyRunBO.Scope scope,
                                                StockSelectionWatchlistDTO.AddRequest request) {
        return item(domainService.add(scope, request.getCandidateId(), request.getWatchlistName(), request.getAttentionReason(),
                request.getInvalidationCondition(), parseDate(request.getNextReviewDate())));
    }

    @Transactional
    public StockSelectionWatchlistDTO.Item review(StockSelectionStrategyRunBO.Scope scope,
                                                   long itemId,
                                                   StockSelectionWatchlistDTO.ReviewRequest request) {
        return item(domainService.review(scope, itemId, request.getDecision(), request.getNote()));
    }

    private StockSelectionWatchlistDTO.Item item(StockSelectionWatchlistBO.Detail value) {
        StockSelectionWatchlistBO.Item item = value.item();
        return StockSelectionWatchlistDTO.Item.builder()
                .id(item.id())
                .watchlistId(item.watchlistId()).watchlistName(item.watchlistName())
                .stockCode(item.stockCode())
                .stockName(item.stockName())
                .sourceStrategyCandidateId(item.sourceCandidateId())
                .status(item.status())
                .attentionReason(item.attentionReason())
                .invalidationCondition(item.invalidationCondition())
                .nextReviewDate(date(item.nextReviewDate()))
                .version(item.version())
                .updatedAt(dateTime(item.updatedAt()))
                .market(item.market()).entryTradeDate(date(item.entryTradeDate())).entryPrice(item.entryPrice())
                .reviews(value.reviews().stream().map(this::review).toList())
                .build();
    }

    private StockSelectionWatchlistDTO.Review review(StockSelectionWatchlistBO.Review value) {
        return StockSelectionWatchlistDTO.Review.builder()
                .id(value.id())
                .decision(value.decision())
                .note(value.note())
                .reviewedAt(dateTime(value.reviewedAt()))
                .build();
    }

    private StockSelectionWatchlistDTO.Performance performance(StockSelectionWatchlistObservationBO.Item value) {
        return StockSelectionWatchlistDTO.Performance.builder()
                .watchlistItemId(value.watchlistItemId())
                .latestClose(value.latestClose())
                .latestTradeDate(date(value.latestTradeDate()))
                .returnSinceEntryPct(value.returnSinceEntryPct())
                .benchmarkReturnPct(value.benchmarkReturnPct())
                .excessReturnPct(value.excessReturnPct())
                .dataSource(value.dataSource())
                .warnings(value.warnings())
                .build();
    }

    private StockSelectionWatchlistDTO.PerformancePoint performancePoint(StockSelectionWatchlistObservationBO.Point value) {
        return StockSelectionWatchlistDTO.PerformancePoint.builder()
                .tradeDate(date(value.tradeDate()))
                .close(value.close())
                .returnSinceEntryPct(value.returnSinceEntryPct())
                .benchmarkReturnPct(value.benchmarkReturnPct())
                .excessReturnPct(value.excessReturnPct())
                .warnings(value.warnings())
                .build();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("nextReviewDate must use ISO-8601 date format");
        }
    }

    private String date(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String dateTime(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
