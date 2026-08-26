package cn.cccs7.cynexus.market.domain.stockselection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class StockSelectionWatchlistDomainServiceTest {

    private static final StockSelectionStrategyRunBO.Scope SCOPE = new StockSelectionStrategyRunBO.Scope("tenant-a", "owner-a");

    @Test
    void addsCandidateOnceAndKeepsExistingActiveItem() {
        CapturingRepository repository = new CapturingRepository();
        repository.candidate = candidate();
        StockSelectionWatchlistDomainService service = service(repository);

        StockSelectionWatchlistBO.Detail first = service.add(SCOPE, 88L, null, "Trend confirmation", "Break below support", LocalDate.of(2026, 8, 20));
        StockSelectionWatchlistBO.Detail duplicate = service.add(SCOPE, 88L, null, "Different reason", null, null);

        assertEquals("WATCHING", first.item().status());
        assertEquals(first.item().id(), duplicate.item().id());
        assertEquals(1, repository.items.size());
        assertEquals("Trend confirmation", duplicate.item().attentionReason());
    }

    @Test
    void refusesCandidateOutsideCurrentIdentityScope() {
        CapturingRepository repository = new CapturingRepository();
        repository.candidate = candidate();
        repository.candidateScope = new StockSelectionStrategyRunBO.Scope("tenant-b", "owner-a");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service(repository).add(SCOPE, 88L, null, "Trend confirmation", null, null));

        assertEquals("strategy candidate is unavailable", error.getMessage());
    }

    @Test
    void recordsAllowedDecisionAndGuardsUnsupportedDecision() {
        CapturingRepository repository = new CapturingRepository();
        repository.candidate = candidate();
        StockSelectionWatchlistDomainService service = service(repository);
        StockSelectionWatchlistBO.Item item = service.add(SCOPE, 88L, null, "Trend confirmation", null, null).item();

        StockSelectionWatchlistBO.Detail paused = service.review(SCOPE, item.id(), "pause", "Wait for volume");

        assertEquals("PAUSED", paused.item().status());
        assertEquals("PAUSE", paused.reviews().getFirst().decision());
        assertThrows(IllegalArgumentException.class, () -> service.review(SCOPE, item.id(), "DELETE", null));
    }

    @Test
    void reportsOptimisticConflictWithoutWritingReview() {
        CapturingRepository repository = new CapturingRepository();
        repository.candidate = candidate();
        StockSelectionWatchlistDomainService service = service(repository);
        StockSelectionWatchlistBO.Item item = service.add(SCOPE, 88L, null, "Trend confirmation", null, null).item();
        repository.updateResult = false;

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.review(SCOPE, item.id(), "CONTINUE", null));

        assertEquals("watchlist item was changed concurrently", error.getMessage());
        assertEquals(0, repository.reviews.size());
    }

    private StockSelectionWatchlistDomainService service(CapturingRepository repository) {
        return new StockSelectionWatchlistDomainService(repository, new SequenceIdGenerator());
    }

    private static StockSelectionWatchlistBO.Candidate candidate() {
        return new StockSelectionWatchlistBO.Candidate(88L, "000001", "Ping An Bank", "SZ", LocalDate.of(2026, 8, 14), java.math.BigDecimal.valueOf(12.34));
    }

    private static final class CapturingRepository implements StockSelectionWatchlistRepository {
        private StockSelectionWatchlistBO.Candidate candidate;
        private StockSelectionStrategyRunBO.Scope candidateScope = SCOPE;
        private final List<StockSelectionWatchlistBO.Item> items = new ArrayList<>();
        private final List<StockSelectionWatchlistBO.Watchlist> watchlists = new ArrayList<>();
        private final List<StockSelectionWatchlistBO.Review> reviews = new ArrayList<>();
        private boolean updateResult = true;

        @Override
        public Optional<StockSelectionWatchlistBO.Candidate> findCandidate(StockSelectionStrategyRunBO.Scope scope, long candidateId) {
            return candidate != null && candidate.id() == candidateId && candidateScope.equals(scope) ? Optional.of(candidate) : Optional.empty();
        }

        @Override
        public Optional<StockSelectionWatchlistBO.Item> findActive(StockSelectionStrategyRunBO.Scope scope, long watchlistId, String stockCode) {
            return items.stream().filter(item -> item.scope().equals(scope) && item.watchlistId() == watchlistId && item.stockCode().equals(stockCode)
                    && (item.status().equals("WATCHING") || item.status().equals("PAUSED"))).findFirst();
        }

        @Override
        public Optional<StockSelectionWatchlistBO.Watchlist> findActiveWatchlist(StockSelectionStrategyRunBO.Scope scope, String name) {
            return watchlists.stream().filter(item -> item.scope().equals(scope) && item.name().equalsIgnoreCase(name)).findFirst();
        }

        @Override
        public void saveWatchlist(StockSelectionWatchlistBO.Watchlist watchlist) {
            watchlists.add(watchlist);
        }

        @Override
        public void save(StockSelectionWatchlistBO.Item item) {
            items.add(item);
        }

        @Override
        public List<StockSelectionWatchlistBO.Detail> list(StockSelectionStrategyRunBO.Scope scope) {
            return items.stream().filter(item -> item.scope().equals(scope))
                    .map(item -> new StockSelectionWatchlistBO.Detail(item,
                            reviews.stream().filter(review -> review.itemId() == item.id()).toList()))
                    .toList();
        }

        @Override
        public boolean update(StockSelectionWatchlistBO.Item item, long expectedVersion) {
            if (!updateResult) {
                return false;
            }
            for (int index = 0; index < items.size(); index++) {
                if (items.get(index).id() == item.id() && items.get(index).version() == expectedVersion) {
                    items.set(index, item);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void saveReview(StockSelectionWatchlistBO.Review review, StockSelectionStrategyRunBO.Scope scope) {
            reviews.add(review);
        }
    }

    private static final class SequenceIdGenerator implements StockSelectionIdGenerator {
        private final AtomicLong sequence = new AtomicLong(100);

        @Override
        public long nextId() {
            return sequence.incrementAndGet();
        }
    }
}
