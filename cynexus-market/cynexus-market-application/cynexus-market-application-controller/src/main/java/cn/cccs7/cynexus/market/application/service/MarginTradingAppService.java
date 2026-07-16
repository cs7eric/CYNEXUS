package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.common.core.result.PageQuery;
import cn.cccs7.cynexus.market.api.dto.MarginTradingDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingIndustriesDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingLatestDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingStocksDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingStockHistoryDTO;
import cn.cccs7.cynexus.market.domain.entity.MarginAccountStatsBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustrySummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginMarketSummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginStockDetailBO;
import cn.cccs7.cynexus.market.domain.repository.MarginTradingRepository;
import cn.cccs7.cynexus.market.domain.margin.MarginTradingDomainPolicy;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MarginTradingAppService {

    private final MarginTradingRepository marginTradingRepository;
    private final MarginTradingDomainPolicy domainPolicy = new MarginTradingDomainPolicy();

    public MarginTradingAppService(MarginTradingRepository marginTradingRepository) {
        this.marginTradingRepository = marginTradingRepository;
    }

    public MarginTradingLatestDTO latest(String tradeDate, int top) {
        LocalDate requestedTradeDate = domainPolicy.parseDate(tradeDate).orElse(null);
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
        int safeTop = domainPolicy.clampLimit(top, MarginTradingDomainPolicy.DEFAULT_TOP_STOCK_LIMIT);
        List<MarginMarketSummaryBO> summaries = marginTradingRepository.listMarketSummaryByTradeDate(servingTradeDate);
        MarginAccountStatsBO account = marginTradingRepository.findAccountStatsByTradeDate(servingTradeDate).orElse(null);
        List<MarginStockDetailBO> topStocks = marginTradingRepository.listStocks(servingTradeDate, null, null, "margin_balance", "desc", 0, safeTop);
        int stockCount = marginTradingRepository.countStocks(servingTradeDate, null, null);
        MarginTradingDomainPolicy.ServingMetadata metadata = domainPolicy.servingMetadata(summaries, account, topStocks);
        return MarginTradingLatestDTO.builder()
                .ok(true)
                .tradeDate(servingTradeDate)
                .requestedTradeDate(requestedTradeDate)
                .fallbackTradeDate(requestedTradeDate != null && !requestedTradeDate.equals(servingTradeDate))
                 .source(metadata.source())
                 .updatedAt(metadata.updatedAt())
                .summaryCount(summaries.size())
                .stockCount(stockCount)
                .summaries(summaries.stream().map(this::summaryDTO).toList())
                .account(accountDTO(account))
                .topStocks(topStocks.stream().map(this::stockDTO).toList())
                .build();
    }

    public MarginTradingHistoryDTO summaryHistory(String start, String end) {
        LocalDate endDate = domainPolicy.parseDate(end).orElse(LocalDate.now());
        LocalDate startDate = domainPolicy.parseDate(start).orElse(endDate.minusDays(MarginTradingDomainPolicy.DEFAULT_HISTORY_DAYS - 1L));
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
        LocalDate requestedTradeDate = domainPolicy.parseDate(tradeDate).orElse(null);
        LocalDate servingTradeDate = marginTradingRepository.findLatestTradeDate(requestedTradeDate).orElse(null);
        PageQuery pageQuery = PageQuery.of(page, pageSize == null ? limit : pageSize,
                MarginTradingDomainPolicy.DEFAULT_STOCK_LIMIT, MarginTradingDomainPolicy.MAX_STOCK_LIMIT);
        String normalizedSort = domainPolicy.normalizeStockSort(sort);
        String normalizedSortDir = domainPolicy.normalizeSortDirection(sortDir);
        String normalizedExchange = domainPolicy.normalizeExchange(exchange);
        String normalizedIndustryCode = domainPolicy.normalizeIndustryCode(industryCode);
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

    public MarginTradingStockHistoryDTO stockHistory(String stockCode, int limit) {
        String normalizedCode = domainPolicy.normalizeStockCode(stockCode);
        List<MarginTradingDTO.StockDetailDTO> items = marginTradingRepository
                .listStockHistory(normalizedCode, domainPolicy.clampLimit(limit, MarginTradingDomainPolicy.DEFAULT_HISTORY_DAYS))
                .stream()
                .map(this::stockDTO)
                .toList();
        return MarginTradingStockHistoryDTO.builder()
                .ok(true)
                .stockCode(normalizedCode)
                .count(items.size())
                .items(items)
                .build();
    }

    public MarginTradingIndustriesDTO industries(String tradeDate, String sort, String sortDir, Integer page, Integer pageSize) {
        LocalDate requestedTradeDate = domainPolicy.parseDate(tradeDate).orElse(null);
        LocalDate servingTradeDate = marginTradingRepository.findLatestTradeDate(requestedTradeDate).orElse(null);
        PageQuery pageQuery = PageQuery.of(page, pageSize, MarginTradingDomainPolicy.DEFAULT_RANKING_PAGE_SIZE,
                MarginTradingDomainPolicy.MAX_STOCK_LIMIT);
        String normalizedSort = domainPolicy.normalizeIndustrySort(sort);
        String normalizedSortDir = domainPolicy.normalizeSortDirection(sortDir);
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

}
