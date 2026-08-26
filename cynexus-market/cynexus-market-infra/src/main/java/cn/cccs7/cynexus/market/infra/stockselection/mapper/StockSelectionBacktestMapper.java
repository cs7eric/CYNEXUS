package cn.cccs7.cynexus.market.infra.stockselection.mapper;

import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionBacktestRunPO;
import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionBacktestTradePO;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockSelectionBacktestMapper {
    int insertRun(StockSelectionBacktestRunPO value);
    int insertTrade(StockSelectionBacktestTradePO value);
    StockSelectionBacktestRunPO findNextQueued();
    int claimRun(@Param("id") long id);
    int completeRun(StockSelectionBacktestRunPO value);
    int failRun(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId, @Param("id") long id,
                @Param("warnings") String warnings, @Param("completedAt") OffsetDateTime completedAt);
    int deleteTrades(@Param("runId") long runId);
    StockSelectionBacktestRunPO findRun(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId, @Param("id") long id);
    StockSelectionBacktestRunPO findLatestCompleted(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId,
                                                    @Param("strategyCode") String strategyCode);
    List<StockSelectionBacktestTradePO> findTrades(@Param("runId") long runId);
}
