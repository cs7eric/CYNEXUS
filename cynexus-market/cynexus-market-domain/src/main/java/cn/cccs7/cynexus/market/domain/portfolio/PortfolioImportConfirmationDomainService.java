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
    private static final String HISTORICAL_DUPLICATE_WARNING =
            "Duplicate trade: symbol, traded time, and quantity already exist in the portfolio ledger.";
    private static final String LEGACY_NON_TODAY_WARNING =
            "Trade date is not today; only today's transactions are valid for import.";
    private final PortfolioCandidateStore candidateStore;
    private final PortfolioTradeRepository tradeRepository;
    private final PortfolioTradeQueryRepository tradeQueryRepository;
    private final PortfolioImportTaskRepository taskRepository;
    private final PortfolioSecuritySymbolResolver securitySymbolResolver;

    public PortfolioImportConfirmationDomainService(PortfolioCandidateStore candidateStore,
                                                    PortfolioTradeRepository tradeRepository,
                                                    PortfolioTradeQueryRepository tradeQueryRepository,
                                                    PortfolioImportTaskRepository taskRepository,
                                                    PortfolioSecuritySymbolResolver securitySymbolResolver) {
        this.candidateStore = candidateStore;
        this.tradeRepository = tradeRepository;
        this.tradeQueryRepository = tradeQueryRepository;
        this.taskRepository = taskRepository;
        this.securitySymbolResolver = securitySymbolResolver;
    }

    public List<PortfolioCandidateReview> candidatesForReview(PortfolioImportTaskBO task,
                                                               List<PortfolioCandidateRecord> candidates) {
        return candidatesForReview(task, candidates, LocalDate.now(SHANGHAI));
    }

    public List<PortfolioCandidateReview> candidatesForReview(PortfolioImportTaskBO task,
                                                               List<PortfolioCandidateRecord> candidates,
                                                               LocalDate today) {
        requireToday(today);
        Map<String, Set<TradeKey>> existingKeys = existingTradeKeys(task, candidates);
        Set<TradeKey> seenKeys = new HashSet<>();
        return candidates.stream().map(candidate -> {
            PortfolioCandidateRecord reviewed = candidate;
            if (!isReviewable(candidate)) return new PortfolioCandidateReview(candidate, null);
            try {
                PortfolioTradeBO trade = toTrade(candidate);
                TradeKey key = tradeKey(trade);
                if (existingKeys.getOrDefault(key.symbol(), Set.of()).contains(key)
                        || !seenKeys.add(key)) {
                    reviewed = excluded(candidate, HISTORICAL_DUPLICATE_WARNING);
                }
            } catch (IllegalArgumentException ignored) {
                // Field validation remains part of the explicit confirm action.
            }
            return new PortfolioCandidateReview(reviewed, suggestedSymbol(reviewed));
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
        Map<String, Set<TradeKey>> existingKeys = existingTradeKeys(task, candidates);
        Set<TradeKey> confirmedKeys = new HashSet<>();
        for (PortfolioCandidateRecord candidate : candidates) {
            if (!selected.isEmpty() && !selected.contains(candidate.id())) continue;
            if (!isReviewable(candidate)) continue;
            PortfolioTradeBO trade;
            try {
                trade = toTrade(candidate);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("candidate row " + candidate.rowNo() + " is invalid: " + exception.getMessage(), exception);
            }
            TradeKey key = tradeKey(trade);
            if (existingKeys.getOrDefault(key.symbol(), Set.of()).contains(key)
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

    private Map<String, Set<TradeKey>> existingTradeKeys(PortfolioImportTaskBO task,
                                                         List<PortfolioCandidateRecord> candidates) {
        Set<String> symbols = new HashSet<>();
        for (PortfolioCandidateRecord candidate : candidates) {
            if (!isReviewable(candidate)) continue;
            try {
                symbols.add(toTrade(candidate).symbol());
            } catch (IllegalArgumentException ignored) {
                // Invalid candidates are handled by the confirm validation path.
            }
        }
        Map<String, Set<TradeKey>> result = new HashMap<>();
        for (String symbol : symbols) {
            Set<TradeKey> keys = new HashSet<>();
            tradeQueryRepository.list(task.getPrincipalKey(), task.getTenantKey(), symbol).stream()
                    .filter(trade -> trade.status() == TradeStatus.ACTIVE)
                    .map(this::tradeKey)
                    .forEach(keys::add);
            result.put(symbol, keys);
        }
        return result;
    }

    private PortfolioCandidateRecord excluded(PortfolioCandidateRecord candidate, String warning) {
        List<String> warnings = new ArrayList<>(candidate.warnings());
        if (!warnings.contains(warning)) warnings.add(warning);
        return new PortfolioCandidateRecord(candidate.id(), candidate.taskId(), candidate.rowNo(),
                candidate.normalizedPayload(), "EXCLUDED", candidate.version(), candidate.confidence(), warnings);
    }

    private boolean isReviewable(PortfolioCandidateRecord candidate) {
        if ("PENDING".equals(candidate.reviewStatus())) return true;
        // Candidates excluded by the former "today only" rule must be
        // re-evaluated against the current ledger under the historical import
        // policy. Other terminal decisions remain immutable.
        return "EXCLUDED".equals(candidate.reviewStatus())
                && candidate.warnings().contains(LEGACY_NON_TODAY_WARNING);
    }

    private TradeKey tradeKey(PortfolioTradeBO trade) {
        // JDBC decimals commonly carry a scale (e.g. 500.00000000), while
        // vision payloads usually do not (500). Canonicalize before using the
        // value in a record key; BigDecimal.equals otherwise treats them as
        // different quantities.
        return new TradeKey(trade.symbol(), trade.tradedAt().atZoneSameInstant(SHANGHAI).toLocalDateTime(),
                trade.quantity().stripTrailingZeros());
    }

    private String suggestedSymbol(PortfolioCandidateRecord candidate) {
        if (!"PENDING".equals(candidate.reviewStatus())) return null;
        JsonNode payload = candidate.normalizedPayload();
        if (optionalText(payload, "symbol") != null) return null;
        String securityName = optionalText(payload, "securityName");
        if (securityName == null) return null;
        PortfolioInstrumentType instrumentType = PortfolioInstrumentType.fromPayload(optionalText(payload, "instrumentType"));
        return securitySymbolResolver.resolveExact(securityName, instrumentType).orElse(null);
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
        // Vision output may append Z to a screenshot wall-clock value. It is
        // not UTC data; interpret it in the exchange's Asia/Shanghai zone so
        // it compares consistently with imported ledger timestamps.
        if (normalized.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(?::\\d{2}(?:\\.\\d{1,9})?)?Z")) {
            normalized = normalized.substring(0, normalized.length() - 1);
            return LocalDateTime.parse(normalized).atZone(SHANGHAI).toOffsetDateTime();
        }
        try { return OffsetDateTime.parse(normalized); }
        catch (Exception ignored) {
            try { return LocalDateTime.parse(normalized).atZone(SHANGHAI).toOffsetDateTime(); }
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

    private record TradeKey(String symbol, LocalDateTime tradedAt, BigDecimal quantity) {
    }
}
