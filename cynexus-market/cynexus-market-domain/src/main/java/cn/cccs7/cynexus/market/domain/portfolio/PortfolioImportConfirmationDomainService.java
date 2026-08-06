package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PortfolioImportConfirmationDomainService {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final String DUPLICATE_WARNING =
            "Duplicate trade: the same symbol and traded time already exists in the portfolio ledger.";
    private static final String NON_TODAY_WARNING =
            "Trade date is not today; only today's transactions are valid for import.";
    private final PortfolioCandidateStore candidateStore;
    private final PortfolioTradeRepository tradeRepository;
    private final PortfolioTradeQueryRepository tradeQueryRepository;
    private final PortfolioImportTaskRepository taskRepository;

    public PortfolioImportConfirmationDomainService(PortfolioCandidateStore candidateStore,
                                                    PortfolioTradeRepository tradeRepository,
                                                    PortfolioTradeQueryRepository tradeQueryRepository,
                                                    PortfolioImportTaskRepository taskRepository) {
        this.candidateStore = candidateStore;
        this.tradeRepository = tradeRepository;
        this.tradeQueryRepository = tradeQueryRepository;
        this.taskRepository = taskRepository;
    }

    public List<PortfolioCandidateRecord> candidatesForReview(PortfolioImportTaskBO task,
                                                               List<PortfolioCandidateRecord> candidates) {
        return candidatesForReview(task, candidates, LocalDate.now(SHANGHAI));
    }

    public List<PortfolioCandidateRecord> candidatesForReview(PortfolioImportTaskBO task,
                                                               List<PortfolioCandidateRecord> candidates,
                                                               LocalDate today) {
        requireToday(today);
        Map<String, Set<Instant>> existingKeys = existingTradeTimes(task, candidates);
        Set<TradeKey> seenKeys = new HashSet<>();
        return candidates.stream().map(candidate -> {
            if (!"PENDING".equals(candidate.reviewStatus())) return candidate;
            try {
                PortfolioTradeBO trade = toTrade(candidate);
                if (!isToday(trade, today)) return excluded(candidate, NON_TODAY_WARNING);
                TradeKey key = new TradeKey(trade.symbol(), trade.tradedAt().toInstant());
                if (existingKeys.getOrDefault(key.symbol(), Set.of()).contains(key.tradedAt())
                        || !seenKeys.add(key)) {
                    return excluded(candidate, DUPLICATE_WARNING);
                }
            } catch (IllegalArgumentException ignored) {
                // Field validation remains part of the explicit confirm action.
            }
            return candidate;
        }).toList();
    }

    public PortfolioImportTaskBO confirm(PortfolioImportTaskBO task, List<Long> candidateIds,
                                         String confirmedBy, OffsetDateTime confirmedAt) {
        return confirm(task, candidateIds, confirmedBy, confirmedAt, LocalDate.now(SHANGHAI));
    }

    public PortfolioImportTaskBO confirm(PortfolioImportTaskBO task, List<Long> candidateIds,
                                         String confirmedBy, OffsetDateTime confirmedAt,
                                         LocalDate today) {
        requireToday(today);
        if (task.getStatus() == PortfolioImportStatus.CONFIRMED) return task;
        if (task.getStatus() != PortfolioImportStatus.READY_FOR_REVIEW) {
            throw new IllegalStateException("portfolio import is not ready for confirmation");
        }
        Set<Long> selected = candidateIds == null ? Set.of() : candidateIds.stream().collect(Collectors.toSet());
        List<PortfolioCandidateRecord> candidates = candidateStore.listByTaskId(task.getId());
        if (candidates.isEmpty()) throw new IllegalArgumentException("portfolio import has no candidates");
        Map<String, Set<Instant>> existingKeys = existingTradeTimes(task, candidates);
        Set<TradeKey> confirmedKeys = new HashSet<>();
        for (PortfolioCandidateRecord candidate : candidates) {
            if (!selected.isEmpty() && !selected.contains(candidate.id())) continue;
            if (!"PENDING".equals(candidate.reviewStatus())) continue;
            PortfolioTradeBO trade;
            try {
                trade = toTrade(candidate);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("candidate row " + candidate.rowNo() + " is invalid: " + exception.getMessage(), exception);
            }
            if (!isToday(trade, today)) continue;
            TradeKey key = new TradeKey(trade.symbol(), trade.tradedAt().toInstant());
            if (existingKeys.getOrDefault(key.symbol(), Set.of()).contains(key.tradedAt())
                    || !confirmedKeys.add(key)) continue;
            if (!tradeRepository.save(task.getPrincipalKey(), task.getTenantKey(), task.getId(), candidate.id(), trade,
                    confirmedBy)) continue;
            if (!candidateStore.markAccepted(candidate.id(), candidate.version())) {
                throw new IllegalStateException("portfolio candidate was changed concurrently: " + candidate.id());
            }
        }
        task.confirm(confirmedAt);
        taskRepository.update(task);
        return task;
    }

    private Map<String, Set<Instant>> existingTradeTimes(PortfolioImportTaskBO task,
                                                         List<PortfolioCandidateRecord> candidates) {
        Set<String> symbols = new HashSet<>();
        for (PortfolioCandidateRecord candidate : candidates) {
            if (!"PENDING".equals(candidate.reviewStatus())) continue;
            try {
                symbols.add(toTrade(candidate).symbol());
            } catch (IllegalArgumentException ignored) {
                // Invalid candidates are handled by the confirm validation path.
            }
        }
        Map<String, Set<Instant>> result = new HashMap<>();
        for (String symbol : symbols) {
            Set<Instant> times = new HashSet<>();
            tradeQueryRepository.list(task.getPrincipalKey(), task.getTenantKey(), symbol).stream()
                    .filter(trade -> trade.status() == TradeStatus.ACTIVE)
                    .map(trade -> trade.tradedAt().toInstant())
                    .forEach(times::add);
            result.put(symbol, times);
        }
        return result;
    }

    private PortfolioCandidateRecord excluded(PortfolioCandidateRecord candidate, String warning) {
        List<String> warnings = new ArrayList<>(candidate.warnings());
        if (!warnings.contains(warning)) warnings.add(warning);
        return new PortfolioCandidateRecord(candidate.id(), candidate.taskId(), candidate.rowNo(),
                candidate.normalizedPayload(), "EXCLUDED", candidate.version(), candidate.confidence(), warnings);
    }

    private boolean isToday(PortfolioTradeBO trade, LocalDate today) {
        return trade.tradedAt().withOffsetSameInstant(ZoneOffset.ofHours(8)).toLocalDate().equals(today);
    }

    private void requireToday(LocalDate today) {
        if (today == null) throw new IllegalArgumentException("today is required");
    }

    private PortfolioTradeBO toTrade(PortfolioCandidateRecord candidate) {
        JsonNode node = candidate.normalizedPayload();
        String symbol = required(node, "symbol");
        if (!symbol.matches("\\d{6}")) throw new IllegalArgumentException("invalid mainland stock or ETF symbol");
        PortfolioInstrumentType instrumentType = PortfolioInstrumentType.fromPayload(optionalText(node, "instrumentType"));
        TradeSide side;
        try { side = TradeSide.valueOf(required(node, "side")); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("invalid trade side", exception); }
        OffsetDateTime tradedAt = parseTime(required(node, "tradedAt"));
        BigDecimal quantity = positive(node, "quantity");
        BigDecimal price = positive(node, "price");
        BigDecimal fee = PortfolioTradingFeePolicy.resolve(instrumentType, quantity, price, optional(node, "fee"));
        String name = optionalText(node, "securityName");
        return new PortfolioTradeBO(0L, symbol, name, instrumentType, side, tradedAt, quantity, price, fee, TradeStatus.ACTIVE);
    }

    private OffsetDateTime parseTime(String value) {
        String normalized = value.trim();
        try { return OffsetDateTime.parse(normalized); }
        catch (Exception ignored) {
            try { return LocalDateTime.parse(normalized).atOffset(ZoneOffset.UTC); }
            catch (Exception localDateTimeIgnored) {
                try {
                    LocalTime.parse(normalized);
                    throw new IllegalArgumentException("tradedAt must include a date; time-only values cannot be confirmed");
                } catch (java.time.format.DateTimeParseException ignoredTimeOnly) {
                    return LocalDate.parse(normalized).atStartOfDay().atOffset(ZoneOffset.UTC);
                }
            }
        }
    }

    private BigDecimal positive(JsonNode node, String field) {
        BigDecimal value = new BigDecimal(required(node, field));
        if (value.signum() <= 0) throw new IllegalArgumentException(field + " must be positive");
        return value;
    }

    private BigDecimal optional(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null) return BigDecimal.ZERO;
        BigDecimal result = new BigDecimal(value);
        if (result.signum() < 0) throw new IllegalArgumentException(field + " must not be negative");
        return result;
    }

    private String required(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null) throw new IllegalArgumentException("candidate field is missing: " + field);
        return value;
    }

    private String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private record TradeKey(String symbol, Instant tradedAt) {
    }
}
