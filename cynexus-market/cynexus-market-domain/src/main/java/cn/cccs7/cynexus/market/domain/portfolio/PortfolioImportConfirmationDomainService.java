package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PortfolioImportConfirmationDomainService {
    private final PortfolioCandidateStore candidateStore;
    private final PortfolioTradeRepository tradeRepository;
    private final PortfolioImportTaskRepository taskRepository;

    public PortfolioImportConfirmationDomainService(PortfolioCandidateStore candidateStore,
                                                    PortfolioTradeRepository tradeRepository,
                                                    PortfolioImportTaskRepository taskRepository) {
        this.candidateStore = candidateStore;
        this.tradeRepository = tradeRepository;
        this.taskRepository = taskRepository;
    }

    public PortfolioImportTaskBO confirm(PortfolioImportTaskBO task, List<Long> candidateIds,
                                         String confirmedBy, OffsetDateTime confirmedAt) {
        if (task.getStatus() == PortfolioImportStatus.CONFIRMED) return task;
        if (task.getStatus() != PortfolioImportStatus.READY_FOR_REVIEW) {
            throw new IllegalStateException("portfolio import is not ready for confirmation");
        }
        Set<Long> selected = candidateIds == null ? Set.of() : candidateIds.stream().collect(Collectors.toSet());
        List<PortfolioCandidateRecord> candidates = candidateStore.listByTaskId(task.getId());
        if (candidates.isEmpty()) throw new IllegalArgumentException("portfolio import has no candidates");
        for (PortfolioCandidateRecord candidate : candidates) {
            if (!selected.isEmpty() && !selected.contains(candidate.id())) continue;
            if (!"PENDING".equals(candidate.reviewStatus())) continue;
            PortfolioTradeBO trade;
            try {
                trade = toTrade(candidate);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("candidate row " + candidate.rowNo() + " is invalid: " + exception.getMessage(), exception);
            }
            tradeRepository.save(task.getPrincipalKey(), task.getTenantKey(), task.getId(), candidate.id(), trade,
                    confirmedBy);
            if (!candidateStore.markAccepted(candidate.id(), candidate.version())) {
                throw new IllegalStateException("portfolio candidate was changed concurrently: " + candidate.id());
            }
        }
        task.confirm(confirmedAt);
        taskRepository.update(task);
        return task;
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
        BigDecimal fee = optional(node, "fee");
        String name = optionalText(node, "securityName");
        return new PortfolioTradeBO(0L, symbol, name, instrumentType, side, tradedAt, quantity, price, fee, TradeStatus.ACTIVE);
    }

    private OffsetDateTime parseTime(String value) {
        try { return OffsetDateTime.parse(value); }
        catch (Exception ignored) {
            try { return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC); }
            catch (Exception localDateTimeIgnored) {
                return LocalDate.parse(value).atStartOfDay().atOffset(ZoneOffset.UTC);
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
}
