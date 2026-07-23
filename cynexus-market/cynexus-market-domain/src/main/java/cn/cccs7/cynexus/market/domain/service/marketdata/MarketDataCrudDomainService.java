package cn.cccs7.cynexus.market.domain.service.marketdata;

import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailyStockBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseSectorDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.SectorFundFlowCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.entity.SectorFundFlowDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.SelfSelectedListBO;
import cn.cccs7.cynexus.market.domain.entity.SelfSelectedListItemBO;
import java.util.List;

public interface MarketDataCrudDomainService {

    List<MarketOverviewBO> listMarketOverviews();

    MarketOverviewBO findMarketOverviewById(Long id);

    Long createMarketOverview(MarketOverviewBO value);

    boolean updateMarketOverview(MarketOverviewBO value);

    boolean deleteMarketOverview(Long id);

    List<MarketLimitDailySnapshotBO> listLimitDailySnapshots();

    MarketLimitDailySnapshotBO findLimitDailySnapshotById(Long id);

    Long createLimitDailySnapshot(MarketLimitDailySnapshotBO value);

    boolean updateLimitDailySnapshot(MarketLimitDailySnapshotBO value);

    boolean deleteLimitDailySnapshot(Long id);

    List<MarketLimitDailyStockBO> listLimitDailyStocks();

    MarketLimitDailyStockBO findLimitDailyStockById(Long id);

    Long createLimitDailyStock(MarketLimitDailyStockBO value);

    boolean updateLimitDailyStock(MarketLimitDailyStockBO value);

    boolean deleteLimitDailyStock(Long id);

    List<MarketPulseCaptureBatchBO> listPulseCaptureBatches();

    MarketPulseCaptureBatchBO findPulseCaptureBatchById(Long id);

    Long createPulseCaptureBatch(MarketPulseCaptureBatchBO value);

    boolean updatePulseCaptureBatch(MarketPulseCaptureBatchBO value);

    boolean deletePulseCaptureBatch(Long id);

    List<MarketPulseSectorDailySnapshotBO> listPulseSectorDailySnapshots();

    MarketPulseSectorDailySnapshotBO findPulseSectorDailySnapshotById(Long id);

    Long createPulseSectorDailySnapshot(MarketPulseSectorDailySnapshotBO value);

    boolean updatePulseSectorDailySnapshot(MarketPulseSectorDailySnapshotBO value);

    boolean deletePulseSectorDailySnapshot(Long id);

    List<SectorFundFlowCaptureBatchBO> listSectorFundFlowCaptureBatches();

    SectorFundFlowCaptureBatchBO findSectorFundFlowCaptureBatchById(Long id);

    Long createSectorFundFlowCaptureBatch(SectorFundFlowCaptureBatchBO value);

    boolean updateSectorFundFlowCaptureBatch(SectorFundFlowCaptureBatchBO value);

    boolean deleteSectorFundFlowCaptureBatch(Long id);

    List<SectorFundFlowDailySnapshotBO> listSectorFundFlowDailySnapshots();

    SectorFundFlowDailySnapshotBO findSectorFundFlowDailySnapshotById(Long id);

    Long createSectorFundFlowDailySnapshot(SectorFundFlowDailySnapshotBO value);

    boolean updateSectorFundFlowDailySnapshot(SectorFundFlowDailySnapshotBO value);

    boolean deleteSectorFundFlowDailySnapshot(Long id);

    List<SelfSelectedListBO> listSelfSelectedLists();

    SelfSelectedListBO findSelfSelectedListById(Long id);

    Long createSelfSelectedList(SelfSelectedListBO value);

    boolean updateSelfSelectedList(SelfSelectedListBO value);

    boolean deleteSelfSelectedList(Long id);

    List<SelfSelectedListItemBO> listSelfSelectedListItems();

    SelfSelectedListItemBO findSelfSelectedListItemById(Long id);

    Long createSelfSelectedListItem(SelfSelectedListItemBO value);

    boolean updateSelfSelectedListItem(SelfSelectedListItemBO value);

    boolean deleteSelfSelectedListItem(Long id);
}
