package cn.cccs7.cynexus.market.domain.stockselection;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class StockSelectionWatchlistDomainService {
    private static final int MAX_NOTE_LENGTH = 1000;

    private final StockSelectionWatchlistRepository repository;
    private final StockSelectionIdGenerator ids;

    public StockSelectionWatchlistDomainService(StockSelectionWatchlistRepository repository, StockSelectionIdGenerator ids) {
        this.repository = repository;
        this.ids = ids;
    }

    public StockSelectionWatchlistBO.Detail add(StockSelectionStrategyRunBO.Scope scope,
                                                 long candidateId,
                                                 String watchlistName,
                                                 String attentionReason,
                                                 String invalidationCondition,
                                                 LocalDate nextReviewDate) {
        if (candidateId <= 0) {
            throw new IllegalArgumentException("candidateId must be positive");
        }
        StockSelectionWatchlistBO.Candidate candidate = repository.findCandidate(scope, candidateId)
                .orElseThrow(() -> new IllegalArgumentException("strategy candidate is unavailable"));
        StockSelectionWatchlistBO.Watchlist watchlist = resolveWatchlist(scope, watchlistName);
        return repository.findActive(scope, watchlist.id(), candidate.stockCode())
                .map(item -> detail(item, List.of()))
                .orElseGet(() -> {
                    StockSelectionWatchlistBO.Item item = new StockSelectionWatchlistBO.Item(
                            ids.nextId(), scope, candidate.stockCode(), candidate.stockName(), candidate.id(), "WATCHING",
                            required(attentionReason, "attentionReason"), optional(invalidationCondition), nextReviewDate,
                            0, OffsetDateTime.now(ZoneOffset.UTC), watchlist.id(), watchlist.name(), candidate.market(),
                            candidate.tradeDate(), candidate.entryPrice());
                    repository.save(item);
                    return detail(item, List.of());
                });
    }

    public List<StockSelectionWatchlistBO.Detail> list(StockSelectionStrategyRunBO.Scope scope) {
        return repository.list(scope);
    }

    public StockSelectionWatchlistBO.Detail review(StockSelectionStrategyRunBO.Scope scope, long itemId, String decision, String note) {
        StockSelectionWatchlistBO.Detail current = repository.list(scope).stream()
                .filter(value -> value.item().id() == itemId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("watchlist item is unavailable"));
        String normalizedDecision = required(decision, "decision").toUpperCase(Locale.ROOT);
        String status = statusFor(normalizedDecision);
        OffsetDateTime reviewedAt = OffsetDateTime.now(ZoneOffset.UTC);
        StockSelectionWatchlistBO.Item currentItem = current.item();
        StockSelectionWatchlistBO.Item updated = new StockSelectionWatchlistBO.Item(
                currentItem.id(), scope, currentItem.stockCode(), currentItem.stockName(), currentItem.sourceCandidateId(),
                status, currentItem.attentionReason(), currentItem.invalidationCondition(), currentItem.nextReviewDate(),
                currentItem.version() + 1, reviewedAt, currentItem.watchlistId(), currentItem.watchlistName(), currentItem.market(),
                currentItem.entryTradeDate(), currentItem.entryPrice());
        if (!repository.update(updated, currentItem.version())) {
            throw new IllegalStateException("watchlist item was changed concurrently");
        }
        StockSelectionWatchlistBO.Review review = new StockSelectionWatchlistBO.Review(
                ids.nextId(), itemId, normalizedDecision, optional(note), reviewedAt);
        repository.saveReview(review, scope);
        return detail(updated, prepend(review, current.reviews()));
    }

    private String statusFor(String decision) {
        return switch (decision) {
            case "CONTINUE" -> "WATCHING";
            case "PAUSE" -> "PAUSED";
            case "INVALIDATE" -> "INVALIDATED";
            case "ARCHIVE" -> "ARCHIVED";
            default -> throw new IllegalArgumentException("unsupported watchlist decision");
        };
    }

    private StockSelectionWatchlistBO.Watchlist resolveWatchlist(StockSelectionStrategyRunBO.Scope scope, String name) {
        String normalized = optional(name);
        normalized = normalized == null ? "My watchlist" : normalized;
        String resolvedName = normalized;
        return repository.findActiveWatchlist(scope, resolvedName).orElseGet(() -> {
            StockSelectionWatchlistBO.Watchlist created = new StockSelectionWatchlistBO.Watchlist(ids.nextId(), scope, resolvedName, 0);
            repository.saveWatchlist(created);
            return created;
        });
    }

    private String required(String value, String field) {
        String normalized = optional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required and must be at most " + MAX_NOTE_LENGTH + " characters");
        }
        return normalized;
    }

    private String optional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("watchlist text must be at most " + MAX_NOTE_LENGTH + " characters");
        }
        return normalized;
    }

    private List<StockSelectionWatchlistBO.Review> prepend(StockSelectionWatchlistBO.Review review,
                                                             List<StockSelectionWatchlistBO.Review> existing) {
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(review), existing.stream()).toList();
    }

    private StockSelectionWatchlistBO.Detail detail(StockSelectionWatchlistBO.Item item,
                                                     List<StockSelectionWatchlistBO.Review> reviews) {
        return new StockSelectionWatchlistBO.Detail(item, reviews);
    }
}
