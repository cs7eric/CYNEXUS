package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailyStockBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseSectorDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.SectorFundFlowCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.entity.SectorFundFlowDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.SelfSelectedListBO;
import cn.cccs7.cynexus.market.domain.entity.SelfSelectedListItemBO;
import cn.cccs7.cynexus.market.domain.service.marketdata.MarketDataCrudDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarketDataCrudAppService {

    private final MarketDataCrudDomainService domainService;

    public MarketDataCrudAppService(MarketDataCrudDomainService domainService) {
        this.domainService = domainService;
    }

    public List<MarketOverviewBO> listMarketOverviews() { return domainService.listMarketOverviews(); }
    public MarketOverviewBO findMarketOverviewById(Long id) { return domainService.findMarketOverviewById(id); }
    public Long createMarketOverview(MarketOverviewBO value) { return domainService.createMarketOverview(value); }
    public boolean updateMarketOverview(MarketOverviewBO value) { return domainService.updateMarketOverview(value); }
    public boolean deleteMarketOverview(Long id) { return domainService.deleteMarketOverview(id); }

    public List<MarketLimitDailySnapshotBO> listLimitDailySnapshots() { return domainService.listLimitDailySnapshots(); }
    public MarketLimitDailySnapshotBO findLimitDailySnapshotById(Long id) { return domainService.findLimitDailySnapshotById(id); }
    public Long createLimitDailySnapshot(MarketLimitDailySnapshotBO value) { return domainService.createLimitDailySnapshot(value); }
    public boolean updateLimitDailySnapshot(MarketLimitDailySnapshotBO value) { return domainService.updateLimitDailySnapshot(value); }
    public boolean deleteLimitDailySnapshot(Long id) { return domainService.deleteLimitDailySnapshot(id); }

    public List<MarketLimitDailyStockBO> listLimitDailyStocks() { return domainService.listLimitDailyStocks(); }
    public MarketLimitDailyStockBO findLimitDailyStockById(Long id) { return domainService.findLimitDailyStockById(id); }
    public Long createLimitDailyStock(MarketLimitDailyStockBO value) { return domainService.createLimitDailyStock(value); }
    public boolean updateLimitDailyStock(MarketLimitDailyStockBO value) { return domainService.updateLimitDailyStock(value); }
    public boolean deleteLimitDailyStock(Long id) { return domainService.deleteLimitDailyStock(id); }

    public List<MarketPulseCaptureBatchBO> listPulseCaptureBatches() { return domainService.listPulseCaptureBatches(); }
    public MarketPulseCaptureBatchBO findPulseCaptureBatchById(Long id) { return domainService.findPulseCaptureBatchById(id); }
    public Long createPulseCaptureBatch(MarketPulseCaptureBatchBO value) { return domainService.createPulseCaptureBatch(value); }
    public boolean updatePulseCaptureBatch(MarketPulseCaptureBatchBO value) { return domainService.updatePulseCaptureBatch(value); }
    public boolean deletePulseCaptureBatch(Long id) { return domainService.deletePulseCaptureBatch(id); }

    public List<MarketPulseSectorDailySnapshotBO> listPulseSectorDailySnapshots() { return domainService.listPulseSectorDailySnapshots(); }
    public MarketPulseSectorDailySnapshotBO findPulseSectorDailySnapshotById(Long id) { return domainService.findPulseSectorDailySnapshotById(id); }
    public Long createPulseSectorDailySnapshot(MarketPulseSectorDailySnapshotBO value) { return domainService.createPulseSectorDailySnapshot(value); }
    public boolean updatePulseSectorDailySnapshot(MarketPulseSectorDailySnapshotBO value) { return domainService.updatePulseSectorDailySnapshot(value); }
    public boolean deletePulseSectorDailySnapshot(Long id) { return domainService.deletePulseSectorDailySnapshot(id); }

    public List<SectorFundFlowCaptureBatchBO> listSectorFundFlowCaptureBatches() { return domainService.listSectorFundFlowCaptureBatches(); }
    public SectorFundFlowCaptureBatchBO findSectorFundFlowCaptureBatchById(Long id) { return domainService.findSectorFundFlowCaptureBatchById(id); }
    public Long createSectorFundFlowCaptureBatch(SectorFundFlowCaptureBatchBO value) { return domainService.createSectorFundFlowCaptureBatch(value); }
    public boolean updateSectorFundFlowCaptureBatch(SectorFundFlowCaptureBatchBO value) { return domainService.updateSectorFundFlowCaptureBatch(value); }
    public boolean deleteSectorFundFlowCaptureBatch(Long id) { return domainService.deleteSectorFundFlowCaptureBatch(id); }

    public List<SectorFundFlowDailySnapshotBO> listSectorFundFlowDailySnapshots() { return domainService.listSectorFundFlowDailySnapshots(); }
    public SectorFundFlowDailySnapshotBO findSectorFundFlowDailySnapshotById(Long id) { return domainService.findSectorFundFlowDailySnapshotById(id); }
    public Long createSectorFundFlowDailySnapshot(SectorFundFlowDailySnapshotBO value) { return domainService.createSectorFundFlowDailySnapshot(value); }
    public boolean updateSectorFundFlowDailySnapshot(SectorFundFlowDailySnapshotBO value) { return domainService.updateSectorFundFlowDailySnapshot(value); }
    public boolean deleteSectorFundFlowDailySnapshot(Long id) { return domainService.deleteSectorFundFlowDailySnapshot(id); }

    public List<SelfSelectedListBO> listSelfSelectedLists() { return domainService.listSelfSelectedLists(); }
    public SelfSelectedListBO findSelfSelectedListById(Long id) { return domainService.findSelfSelectedListById(id); }
    public Long createSelfSelectedList(SelfSelectedListBO value) { return domainService.createSelfSelectedList(value); }
    public boolean updateSelfSelectedList(SelfSelectedListBO value) { return domainService.updateSelfSelectedList(value); }
    public boolean deleteSelfSelectedList(Long id) { return domainService.deleteSelfSelectedList(id); }

    public List<SelfSelectedListItemBO> listSelfSelectedListItems() { return domainService.listSelfSelectedListItems(); }
    public SelfSelectedListItemBO findSelfSelectedListItemById(Long id) { return domainService.findSelfSelectedListItemById(id); }
    public Long createSelfSelectedListItem(SelfSelectedListItemBO value) { return domainService.createSelfSelectedListItem(value); }
    public boolean updateSelfSelectedListItem(SelfSelectedListItemBO value) { return domainService.updateSelfSelectedListItem(value); }
    public boolean deleteSelfSelectedListItem(Long id) { return domainService.deleteSelfSelectedListItem(id); }
}
