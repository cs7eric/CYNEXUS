package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.common.core.result.PageQuery;
import cn.cccs7.cynexus.market.api.dto.MarginTradingDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingIndustriesDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingLatestDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingStocksDTO;
import cn.cccs7.cynexus.market.domain.entity.MarginAccountStatsBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustrySummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginMarketSummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginStockDetailBO;
import cn.cccs7.cynexus.market.domain.repository.MarginTradingRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class MarginTradingAppService {

    private static final int DEFAULT_TOP_STOCK_LIMIT = 20;
    private static final int DEFAULT_STOCK_LIMIT = 100;
    private static final int DEFAULT_RANKING_PAGE_SIZE = 20;
    private static final int MAX_STOCK_LIMIT = 500;
    private static final int DEFAULT_HISTORY_DAYS = 120;

    private final MarginTradingRepository marginTradingRepository;

    public MarginTradingAppService(MarginTradingRepository marginTradingRepository) {
        this.marginTradingRepository = marginTradingRepository;
    }

    public MarginTradingLatestDTO latest(String tradeDate, int top) {
        LocalDate requestedTradeDate = parseDate(tradeDate).orElse(null);
        LocalDate servingTradeDate = marginTradingRepository.findLatestTradeDate(requestedTradeDate).orElse(null);
        if (servingTradeDate == null) {
            return MarginTradingLatestDTO.builder()
                    .ok(false)
                    .requestedTradeDate(requestedTradeDate)
                    .fallbackTradeDate(false)
                    .summaryCount(0)
                    .stockCount(0)
                    .summaries(List.of())
                    .topStocks(List.of())
                    .error("margin trading data is empty")
                    .build();
        }
        int safeTop = clampLimit(top, DEFAULT_TOP_STOCK_LIMIT);
        List<MarginMarketSummaryBO> summaries = marginTradingRepository.listMarketSummaryByTradeDate(servingTradeDate);
        MarginAccountStatsBO account = marginTradingRepository.findAccountStatsByTradeDate(servingTradeDate).orElse(null);
        List<MarginStockDetailBO> topStocks = marginTradingRepository.listStocks(servingTradeDate, null, null, "margin_balance", "desc", 0, safeTop);
        int stockCount = marginTradingRepository.countStocks(servingTradeDate, null, null);
        return MarginTradingLatestDTO.builder()
                .ok(true)
                .tradeDate(servingTradeDate)
                .requestedTradeDate(requestedTradeDate)
                .fallbackTradeDate(requestedTradeDate != null && !requestedTradeDate.equals(servingTradeDate))
                .source(resolveSource(summaries, account))
                .updatedAt(resolveUpdatedAt(summaries, account, topStocks))
                .summaryCount(summaries.size())
                .stockCount(stockCount)
                .summaries(summaries.stream().map(this::summaryDTO).toList())
                .account(accountDTO(account))
                .topStocks(topStocks.stream().map(this::stockDTO).toList())
                .build();
    }

    public MarginTradingHistoryDTO summaryHistory(String start, String end) {
        LocalDate endDate = parseDate(end).orElse(LocalDate.now());
        LocalDate startDate = parseDate(start).orElse(endDate.minusDays(DEFAULT_HISTORY_DAYS - 1L));
        if (startDate.isAfter(endDate)) {
            LocalDate previousStart = startDate;
            startDate = endDate;
            endDate = previousStart;
        }
        List<MarginTradingDTO.MarketSummaryDTO> items = marginTradingRepository.listMarketSummaryHistory(startDate, endDate)
                .stream()
                .map(this::summaryDTO)
                .toList();
        return MarginTradingHistoryDTO.builder()
                .ok(true)
                .start(startDate)
                .end(endDate)
                .count(items.size())
                .items(items)
                .build();
    }

    public MarginTradingStocksDTO stocks(String tradeDate,
                                         String exchange,
                                         String industryCode,
                                         String sort,
                                         String sortDir,
                                         Integer page,
                                         Integer pageSize,
                                         int limit) {
        LocalDate requestedTradeDate = parseDate(tradeDate).orElse(null);
        LocalDate servingTradeDate = marginTradingRepository.findLatestTradeDate(requestedTradeDate).orElse(null);
        PageQuery pageQuery = PageQuery.of(page, pageSize == null ? limit : pageSize, DEFAULT_STOCK_LIMIT, MAX_STOCK_LIMIT);
        String normalizedSort = normalizeStockSort(sort);
        String normalizedSortDir = normalizeSortDir(sortDir);
        String normalizedExchange = normalizeExchange(exchange);
        String normalizedIndustryCode = normalizeIndustryCode(industryCode);
        if (servingTradeDate == null) {
            return MarginTradingStocksDTO.builder()
                    .ok(false)
                    .tradeDate(requestedTradeDate)
                    .exchange(normalizedExchange)
                    .industryCode(normalizedIndustryCode)
                    .sort(normalizedSort)
                    .sortDir(normalizedSortDir)
                    .page(pageQuery.getPageNo())
                    .pageSize(pageQuery.getPageSize())
                    .limit(pageQuery.getPageSize())
                    .count(0)
                    .items(List.of())
                    .error("margin stock detail data is empty")
                    .build();
        }
        List<MarginTradingDTO.StockDetailDTO> items = marginTradingRepository
                .listStocks(servingTradeDate, normalizedExchange, normalizedIndustryCode, normalizedSort, normalizedSortDir, pageQuery.getOffset(), pageQuery.getPageSize())
                .stream()
                .map(this::stockDTO)
                .toList();
        return MarginTradingStocksDTO.builder()
                .ok(true)
                .tradeDate(servingTradeDate)
                .exchange(normalizedExchange)
                .industryCode(normalizedIndustryCode)
                .sort(normalizedSort)
                .sortDir(normalizedSortDir)
                .page(pageQuery.getPageNo())
                .pageSize(pageQuery.getPageSize())
                .limit(pageQuery.getPageSize())
                .count(marginTradingRepository.countStocks(servingTradeDate, normalizedExchange, normalizedIndustryCode))
                .items(items)
                .build();
    }

    public MarginTradingIndustriesDTO industries(String tradeDate, String sort, String sortDir, Integer page, Integer pageSize) {
        LocalDate requestedTradeDate = parseDate(tradeDate).orElse(null);
        LocalDate servingTradeDate = marginTradingRepository.findLatestTradeDate(requestedTradeDate).orElse(null);
        PageQuery pageQuery = PageQuery.of(page, pageSize, DEFAULT_RANKING_PAGE_SIZE, MAX_STOCK_LIMIT);
        String normalizedSort = normalizeIndustrySort(sort);
        String normalizedSortDir = normalizeSortDir(sortDir);
        if (servingTradeDate == null) {
            return MarginTradingIndustriesDTO.builder()
                    .ok(false)
                    .tradeDate(requestedTradeDate)
                    .sort(normalizedSort)
                    .sortDir(normalizedSortDir)
                    .page(pageQuery.getPageNo())
                    .pageSize(pageQuery.getPageSize())
                    .limit(pageQuery.getPageSize())
                    .count(0)
                    .items(List.of())
                    .error("margin stock detail data is empty")
                    .build();
        }
        List<MarginTradingDTO.IndustrySummaryDTO> items = marginTradingRepository
                .listIndustrySummaries(servingTradeDate, normalizedSort, normalizedSortDir, pageQuery.getOffset(), pageQuery.getPageSize())
                .stream()
                .map(this::industryDTO)
                .toList();
        return MarginTradingIndustriesDTO.builder()
                .ok(true)
                .tradeDate(servingTradeDate)
                .constituentSnapshotDate(items.isEmpty() ? null : items.getFirst().getConstituentSnapshotDate())
                .sort(normalizedSort)
                .sortDir(normalizedSortDir)
                .page(pageQuery.getPageNo())
                .pageSize(pageQuery.getPageSize())
                .limit(pageQuery.getPageSize())
                .count(marginTradingRepository.countIndustrySummaries(servingTradeDate))
                .items(items)
                .build();
    }

    private MarginTradingDTO.MarketSummaryDTO summaryDTO(MarginMarketSummaryBO row) {
        return MarginTradingDTO.MarketSummaryDTO.builder()
                .tradeDate(row.getTradeDate())
                .exchange(row.getExchange())
                .source(row.getSource())
                .financingBalance(row.getFinancingBalance())
                .financingBuyAmount(row.getFinancingBuyAmount())
                .financingRepayAmount(row.getFinancingRepayAmount())
                .securitiesLendingBalance(row.getSecuritiesLendingBalance())
                .securitiesLendingBalanceAmount(row.getSecuritiesLendingBalanceAmount())
                .securitiesLendingSellVolume(row.getSecuritiesLendingSellVolume())
                .securitiesLendingRepayVolume(row.getSecuritiesLendingRepayVolume())
                .marginBalance(row.getMarginBalance())
                .extra(row.getExtra())
                .updatedAt(row.getUpdatedAt())
                .build();
    }

    private MarginTradingDTO.StockDetailDTO stockDTO(MarginStockDetailBO row) {
        return MarginTradingDTO.StockDetailDTO.builder()
                .tradeDate(row.getTradeDate())
                .exchange(row.getExchange())
                .stockCode(row.getStockCode())
                .stockName(row.getStockName())
                .source(row.getSource())
                .financingBalance(row.getFinancingBalance())
                .financingBuyAmount(row.getFinancingBuyAmount())
                .financingRepayAmount(row.getFinancingRepayAmount())
                .securitiesLendingBalance(row.getSecuritiesLendingBalance())
                .securitiesLendingBalanceAmount(row.getSecuritiesLendingBalanceAmount())
                .securitiesLendingSellVolume(row.getSecuritiesLendingSellVolume())
                .securitiesLendingRepayVolume(row.getSecuritiesLendingRepayVolume())
                .marginBalance(row.getMarginBalance())
                .extra(row.getExtra())
                .updatedAt(row.getUpdatedAt())
                .build();
    }

    private MarginTradingDTO.IndustrySummaryDTO industryDTO(MarginIndustrySummaryBO row) {
        return MarginTradingDTO.IndustrySummaryDTO.builder()
                .tradeDate(row.getTradeDate())
                .constituentSnapshotDate(row.getConstituentSnapshotDate())
                .industryCode(row.getIndustryCode())
                .industryName(row.getIndustryName())
                .tdxCode(row.getTdxCode())
                .tdxName(row.getTdxName())
                .constituentCount(row.getConstituentCount())
                .matchedStockCount(row.getMatchedStockCount())
                .financingBalance(row.getFinancingBalance())
                .financingBuyAmount(row.getFinancingBuyAmount())
                .financingRepayAmount(row.getFinancingRepayAmount())
                .securitiesLendingBalance(row.getSecuritiesLendingBalance())
                .securitiesLendingBalanceAmount(row.getSecuritiesLendingBalanceAmount())
                .securitiesLendingSellVolume(row.getSecuritiesLendingSellVolume())
                .securitiesLendingRepayVolume(row.getSecuritiesLendingRepayVolume())
                .marginBalance(row.getMarginBalance())
                .build();
    }

    private MarginTradingDTO.AccountStatsDTO accountDTO(MarginAccountStatsBO row) {
        if (row == null) {
            return null;
        }
        return MarginTradingDTO.AccountStatsDTO.builder()
                .tradeDate(row.getTradeDate())
                .source(row.getSource())
                .financingBalance(row.getFinancingBalance())
                .securitiesLendingBalance(row.getSecuritiesLendingBalance())
                .financingBuyAmount(row.getFinancingBuyAmount())
                .securitiesLendingSellAmount(row.getSecuritiesLendingSellAmount())
                .brokerCount(row.getBrokerCount())
                .businessDepartmentCount(row.getBusinessDepartmentCount())
                .personalInvestorCount(row.getPersonalInvestorCount())
                .institutionInvestorCount(row.getInstitutionInvestorCount())
                .activeInvestorCount(row.getActiveInvestorCount())
                .liabilityInvestorCount(row.getLiabilityInvestorCount())
                .totalGuarantee(row.getTotalGuarantee())
                .averageGuaranteeRatio(row.getAverageGuaranteeRatio())
                .extra(row.getExtra())
                .updatedAt(row.getUpdatedAt())
                .build();
    }

    private Optional<LocalDate> parseDate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim();
        if (normalized.length() == 8 && !normalized.contains("-")) {
            normalized = normalized.substring(0, 4) + "-" + normalized.substring(4, 6) + "-" + normalized.substring(6, 8);
        }
        return Optional.of(LocalDate.parse(normalized));
    }

    private int clampLimit(int limit, int fallback) {
        int value = limit <= 0 ? fallback : limit;
        return Math.max(1, Math.min(value, MAX_STOCK_LIMIT));
    }

    private String normalizeExchange(String exchange) {
        if (exchange == null || exchange.isBlank()) {
            return null;
        }
        return exchange.trim().toUpperCase();
    }

    private String normalizeIndustryCode(String industryCode) {
        if (industryCode == null || industryCode.isBlank()) {
            return null;
        }
        return industryCode.trim();
    }

    private String normalizeStockSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "margin_balance";
        }
        return switch (sort.trim()) {
            case "stock_code", "stock_name", "exchange",
                    "financing_balance", "financing_buy_amount", "financing_repay_amount",
                    "securities_lending_balance", "securities_lending_balance_amount", "margin_balance" -> sort.trim();
            default -> "margin_balance";
        };
    }

    private String normalizeIndustrySort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "margin_balance";
        }
        return switch (sort.trim()) {
            case "industry_code", "industry_name", "tdx_code", "tdx_name", "constituent_count", "matched_stock_count",
                    "financing_balance", "financing_buy_amount", "financing_repay_amount",
                    "securities_lending_balance", "securities_lending_balance_amount", "margin_balance" -> sort.trim();
            default -> "margin_balance";
        };
    }

    private String normalizeSortDir(String sortDir) {
        if (sortDir == null || sortDir.isBlank()) {
            return "desc";
        }
        return "asc".equalsIgnoreCase(sortDir.trim()) ? "asc" : "desc";
    }

    private String resolveSource(List<MarginMarketSummaryBO> summaries, MarginAccountStatsBO account) {
        return summaries.stream()
                .map(MarginMarketSummaryBO::getSource)
                .filter(item -> item != null && !item.isBlank())
                .findFirst()
                .orElse(account == null ? null : account.getSource());
    }

    private OffsetDateTime resolveUpdatedAt(List<MarginMarketSummaryBO> summaries,
                                            MarginAccountStatsBO account,
                                            List<MarginStockDetailBO> stocks) {
        Optional<OffsetDateTime> summaryUpdatedAt = summaries.stream()
                .map(MarginMarketSummaryBO::getUpdatedAt)
                .filter(item -> item != null)
                .max(Comparator.naturalOrder());
        Optional<OffsetDateTime> stockUpdatedAt = stocks.stream()
                .map(MarginStockDetailBO::getUpdatedAt)
                .filter(item -> item != null)
                .max(Comparator.naturalOrder());
        return Stream.of(summaryUpdatedAt.orElse(null), account == null ? null : account.getUpdatedAt(), stockUpdatedAt.orElse(null))
                .filter(item -> item != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
