package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.market.infra.basic.entity.MarginAccountStatsPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginIndustrySummaryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginMarketSummaryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginStockDetailPO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MarginTradingMapper {

    LocalDate findLatestTradeDate(@Param("endDate") LocalDate endDate);

    List<MarginMarketSummaryPO> listMarketSummaryByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    MarginAccountStatsPO findAccountStatsByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    List<MarginMarketSummaryPO> listMarketSummaryHistory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<MarginStockDetailPO> listStocks(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("exchange") String exchange,
            @Param("industryCode") String industryCode,
            @Param("sort") String sort,
            @Param("sortDir") String sortDir,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countStocks(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("exchange") String exchange,
            @Param("industryCode") String industryCode
    );

    List<MarginStockDetailPO> listStockHistory(
            @Param("stockCode") String stockCode,
            @Param("limit") int limit
    );

    List<MarginIndustrySummaryPO> listIndustrySummaries(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("sort") String sort,
            @Param("sortDir") String sortDir,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countIndustrySummaries(@Param("tradeDate") LocalDate tradeDate);
}
