package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.MarginTradingDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingIndustriesDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingIndustryHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingLatestDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingStocksDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingStockHistoryDTO;
import cn.cccs7.cynexus.market.domain.entity.MarginAccountStatsBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustrySummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustryHistoryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginMarketSummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginStockDetailBO;
import cn.cccs7.cynexus.market.domain.entity.margin.MarginTradingQueryBO;
import cn.cccs7.cynexus.market.domain.service.margin.MarginTradingQueryDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarginTradingAppService {

    private final MarginTradingQueryDomainService domainService;

    public MarginTradingAppService(MarginTradingQueryDomainService domainService) {
        this.domainService = domainService;
    }

    public MarginTradingLatestDTO latest(String tradeDate, int top) {
        MarginTradingQueryBO.Latest value = domainService.latest(tradeDate, top);
        if (!value.ok()) {
            return MarginTradingLatestDTO.builder()
                    .ok(false)
                    .requestedTradeDate(value.requestedTradeDate())
                    .fallbackTradeDate(false)
                    .summaryCount(0)
                    .stockCount(0)
                    .summaries(List.of())
                    .topStocks(List.of())
                    .error(value.error())
                    .build();
        }
        return MarginTradingLatestDTO.builder()
                .ok(true)
                .tradeDate(value.tradeDate()).requestedTradeDate(value.requestedTradeDate())
                .fallbackTradeDate(value.fallbackTradeDate()).source(value.source()).updatedAt(value.updatedAt())
                .summaryCount(value.summaries().size()).stockCount(value.stockCount())
                .summaries(value.summaries().stream().map(this::summaryDTO).toList())
                .account(accountDTO(value.account()))
                .topStocks(value.topStocks().stream().map(this::stockDTO).toList())
                .build();
    }

    public MarginTradingHistoryDTO summaryHistory(String start, String end) {
        MarginTradingQueryBO.History value = domainService.summaryHistory(start, end);
        List<MarginTradingDTO.MarketSummaryDTO> items = value.items().stream()
                .map(this::summaryDTO)
                .toList();
        return MarginTradingHistoryDTO.builder()
                .ok(true)
                .start(value.start()).end(value.end())
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
        MarginTradingQueryBO.Stocks value = domainService.stocks(tradeDate, exchange, industryCode, sort, sortDir,
                page, pageSize, limit);
        if (!value.ok()) {
            return MarginTradingStocksDTO.builder()
                    .ok(false)
                    .tradeDate(value.tradeDate()).exchange(value.exchange()).industryCode(value.industryCode())
                    .sort(value.sort()).sortDir(value.sortDir()).page(value.page().number())
                    .pageSize(value.page().size()).limit(value.page().size())
                    .count(0)
                    .items(List.of())
                    .error(value.error())
                    .build();
        }
        List<MarginTradingDTO.StockDetailDTO> items = value.items()
                .stream()
                .map(this::stockDTO)
                .toList();
        return MarginTradingStocksDTO.builder()
                .ok(true)
                .tradeDate(value.tradeDate()).exchange(value.exchange()).industryCode(value.industryCode())
                .sort(value.sort()).sortDir(value.sortDir()).page(value.page().number())
                .pageSize(value.page().size()).limit(value.page().size()).count(value.count())
                .items(items)
                .build();
    }

    public MarginTradingStockHistoryDTO stockHistory(String stockCode, int limit) {
        MarginTradingQueryBO.StockHistory value = domainService.stockHistory(stockCode, limit);
        List<MarginTradingDTO.StockDetailDTO> items = value.items()
                .stream()
                .map(this::stockDTO)
                .toList();
        return MarginTradingStockHistoryDTO.builder()
                .ok(true)
                .stockCode(value.stockCode())
                .count(items.size())
                .items(items)
                .build();
    }

    public MarginTradingIndustriesDTO industries(String tradeDate, String sort, String sortDir, Integer page, Integer pageSize) {
        MarginTradingQueryBO.Industries value = domainService.industries(tradeDate, sort, sortDir, page, pageSize);
        if (!value.ok()) {
            return MarginTradingIndustriesDTO.builder()
                    .ok(false)
                    .tradeDate(value.tradeDate()).sort(value.sort()).sortDir(value.sortDir())
                    .page(value.page().number()).pageSize(value.page().size()).limit(value.page().size())
                    .count(0)
                    .items(List.of())
                    .error(value.error())
                    .build();
        }
        List<MarginTradingDTO.IndustrySummaryDTO> items = value.items()
                .stream()
                .map(this::industryDTO)
                .toList();
        return MarginTradingIndustriesDTO.builder()
                .ok(true)
                .tradeDate(value.tradeDate()).constituentSnapshotDate(value.constituentSnapshotDate())
                .sort(value.sort()).sortDir(value.sortDir()).page(value.page().number())
                .pageSize(value.page().size()).limit(value.page().size()).count(value.count())
                .items(items)
                .build();
    }

    public MarginTradingIndustryHistoryDTO industryHistory(String industryCode, String tradeDate, int limit) {
        MarginTradingQueryBO.IndustryHistory value = domainService.industryHistory(industryCode, tradeDate, limit);
        List<MarginTradingDTO.IndustryHistoryDTO> items = value.items().stream()
                .map(this::industryHistoryDTO)
                .toList();
        return MarginTradingIndustryHistoryDTO.builder()
                .ok(value.ok())
                .error(value.error())
                .industryCode(value.industryCode())
                .tradeDate(value.tradeDate() == null ? null : value.tradeDate().toString())
                .count(items.size())
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
                .turnoverAmount(row.getTurnoverAmount())
                .build();
    }

    private MarginTradingDTO.IndustryHistoryDTO industryHistoryDTO(MarginIndustryHistoryBO row) {
        return MarginTradingDTO.IndustryHistoryDTO.builder()
                .tradeDate(row.getTradeDate())
                .constituentSnapshotDate(row.getConstituentSnapshotDate())
                .industryCode(row.getIndustryCode())
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
                .turnoverAmount(row.getTurnoverAmount())
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
