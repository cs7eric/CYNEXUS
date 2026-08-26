package cn.cccs7.cynexus.market.infra.stockselection;

import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionWatchlistBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionWatchlistRepository;
import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionWatchlistItemPO;
import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionWatchlistReviewPO;
import cn.cccs7.cynexus.market.infra.stockselection.mapper.StockSelectionWatchlistMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class StockSelectionWatchlistRepositoryAdapter implements StockSelectionWatchlistRepository {

    private final StockSelectionWatchlistMapper mapper;

    public StockSelectionWatchlistRepositoryAdapter(StockSelectionWatchlistMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<StockSelectionWatchlistBO.Candidate> findCandidate(StockSelectionStrategyRunBO.Scope scope, long candidateId) {
        Map<String, Object> value = mapper.findCandidate(scope.tenantId(), scope.ownerId(), candidateId);
        if (value == null || !(value.get("stockCode") instanceof String stockCode) || stockCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new StockSelectionWatchlistBO.Candidate(candidateId, stockCode, (String) value.get("stockName"),
                (String) value.get("market"), (java.time.LocalDate) value.get("tradeDate"), (java.math.BigDecimal) value.get("entryPrice")));
    }

    @Override
    public Optional<StockSelectionWatchlistBO.Item> findActive(StockSelectionStrategyRunBO.Scope scope, long watchlistId, String stockCode) {
        return Optional.ofNullable(mapper.findActive(scope.tenantId(), scope.ownerId(), watchlistId, stockCode)).map(value -> item(scope, value));
    }

    @Override public Optional<StockSelectionWatchlistBO.Watchlist> findActiveWatchlist(StockSelectionStrategyRunBO.Scope scope, String name) {
        Map<String, Object> value = mapper.findActiveWatchlist(scope.tenantId(), scope.ownerId(), name);
        return value == null ? Optional.empty() : Optional.of(new StockSelectionWatchlistBO.Watchlist(((Number) value.get("id")).longValue(), scope, (String) value.get("name"), ((Number) value.get("version")).longValue()));
    }
    @Override public void saveWatchlist(StockSelectionWatchlistBO.Watchlist value) { mapper.insertWatchlist(value.id(), value.scope().tenantId(), value.scope().ownerId(), value.name(), value.version()); }

    @Override
    public void save(StockSelectionWatchlistBO.Item item) {
        mapper.insert(po(item));
    }

    @Override
    public List<StockSelectionWatchlistBO.Detail> list(StockSelectionStrategyRunBO.Scope scope) {
        Map<Long, List<StockSelectionWatchlistBO.Review>> reviewsByItem = new HashMap<>();
        for (StockSelectionWatchlistReviewPO review : mapper.reviews(scope.tenantId(), scope.ownerId())) {
            reviewsByItem.computeIfAbsent(review.getWatchlistItemId(), ignored -> new ArrayList<>())
                    .add(new StockSelectionWatchlistBO.Review(review.getId(), review.getWatchlistItemId(), review.getDecision(),
                            review.getNote(), review.getReviewedAt()));
        }
        return mapper.list(scope.tenantId(), scope.ownerId()).stream()
                .map(value -> new StockSelectionWatchlistBO.Detail(item(scope, value), reviewsByItem.getOrDefault(value.getId(), List.of())))
                .toList();
    }

    @Override
    public boolean update(StockSelectionWatchlistBO.Item item, long expectedVersion) {
        return mapper.update(po(item), expectedVersion) == 1;
    }

    @Override
    public void saveReview(StockSelectionWatchlistBO.Review review, StockSelectionStrategyRunBO.Scope scope) {
        StockSelectionWatchlistReviewPO value = new StockSelectionWatchlistReviewPO();
        value.setId(review.id());
        value.setWatchlistItemId(review.itemId());
        value.setTenantId(scope.tenantId());
        value.setOwnerId(scope.ownerId());
        value.setDecision(review.decision());
        value.setNote(review.note());
        value.setReviewedAt(review.reviewedAt());
        mapper.insertReview(value);
    }

    private StockSelectionWatchlistBO.Item item(StockSelectionStrategyRunBO.Scope scope, StockSelectionWatchlistItemPO value) {
        return new StockSelectionWatchlistBO.Item(value.getId(), scope, value.getStockCode(), value.getStockName(),
                value.getSourceStrategyCandidateId(), value.getStatus(), value.getAttentionReason(), value.getInvalidationCondition(),
                value.getNextReviewDate(), value.getVersion(), value.getUpdatedAt(), value.getWatchlistId(), value.getWatchlistName(),
                value.getMarket(), value.getEntryTradeDate(), value.getEntryPrice());
    }

    private StockSelectionWatchlistItemPO po(StockSelectionWatchlistBO.Item item) {
        StockSelectionWatchlistItemPO value = new StockSelectionWatchlistItemPO();
        value.setId(item.id());
        value.setWatchlistId(item.watchlistId()); value.setWatchlistName(item.watchlistName());
        value.setTenantId(item.scope().tenantId());
        value.setOwnerId(item.scope().ownerId());
        value.setStockCode(item.stockCode());
        value.setStockName(item.stockName());
        value.setMarket(item.market()); value.setEntryTradeDate(item.entryTradeDate()); value.setEntryPrice(item.entryPrice());
        value.setSourceStrategyCandidateId(item.sourceCandidateId());
        value.setStatus(item.status());
        value.setAttentionReason(item.attentionReason());
        value.setInvalidationCondition(item.invalidationCondition());
        value.setNextReviewDate(item.nextReviewDate());
        value.setVersion(item.version());
        value.setUpdatedAt(item.updatedAt());
        return value;
    }
}
