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
import cn.cccs7.cynexus.market.domain.repository.MarketLimitDailySnapshotRepository;
import cn.cccs7.cynexus.market.domain.repository.MarketLimitDailyStockRepository;
import cn.cccs7.cynexus.market.domain.repository.MarketOverviewRepository;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseCaptureBatchRepository;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseSectorDailySnapshotRepository;
import cn.cccs7.cynexus.market.domain.repository.SectorFundFlowCaptureBatchRepository;
import cn.cccs7.cynexus.market.domain.repository.SectorFundFlowDailySnapshotRepository;
import cn.cccs7.cynexus.market.domain.repository.SelfSelectedListItemRepository;
import cn.cccs7.cynexus.market.domain.repository.SelfSelectedListRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarketDataCrudAppService {

    private final MarketOverviewRepository marketOverviewRepository;
    private final MarketLimitDailySnapshotRepository marketLimitDailySnapshotRepository;
    private final MarketLimitDailyStockRepository marketLimitDailyStockRepository;
    private final MarketPulseCaptureBatchRepository marketPulseCaptureBatchRepository;
    private final MarketPulseSectorDailySnapshotRepository marketPulseSectorDailySnapshotRepository;
    private final SectorFundFlowCaptureBatchRepository sectorFundFlowCaptureBatchRepository;
    private final SectorFundFlowDailySnapshotRepository sectorFundFlowDailySnapshotRepository;
    private final SelfSelectedListRepository selfSelectedListRepository;
    private final SelfSelectedListItemRepository selfSelectedListItemRepository;

    public MarketDataCrudAppService(
            MarketOverviewRepository marketOverviewRepository,
            MarketLimitDailySnapshotRepository marketLimitDailySnapshotRepository,
            MarketLimitDailyStockRepository marketLimitDailyStockRepository,
            MarketPulseCaptureBatchRepository marketPulseCaptureBatchRepository,
            MarketPulseSectorDailySnapshotRepository marketPulseSectorDailySnapshotRepository,
            SectorFundFlowCaptureBatchRepository sectorFundFlowCaptureBatchRepository,
            SectorFundFlowDailySnapshotRepository sectorFundFlowDailySnapshotRepository,
            SelfSelectedListRepository selfSelectedListRepository,
            SelfSelectedListItemRepository selfSelectedListItemRepository) {
        this.marketOverviewRepository = marketOverviewRepository;
        this.marketLimitDailySnapshotRepository = marketLimitDailySnapshotRepository;
        this.marketLimitDailyStockRepository = marketLimitDailyStockRepository;
        this.marketPulseCaptureBatchRepository = marketPulseCaptureBatchRepository;
        this.marketPulseSectorDailySnapshotRepository = marketPulseSectorDailySnapshotRepository;
        this.sectorFundFlowCaptureBatchRepository = sectorFundFlowCaptureBatchRepository;
        this.sectorFundFlowDailySnapshotRepository = sectorFundFlowDailySnapshotRepository;
        this.selfSelectedListRepository = selfSelectedListRepository;
        this.selfSelectedListItemRepository = selfSelectedListItemRepository;
    }

    public List<MarketOverviewBO> listMarketOverviews() {
        return marketOverviewRepository.listAll();
    }

    public MarketOverviewBO findMarketOverviewById(Long id) {
        return marketOverviewRepository.findById(id);
    }

    public Long createMarketOverview(MarketOverviewBO value) {
        return marketOverviewRepository.create(value);
    }

    public boolean updateMarketOverview(MarketOverviewBO value) {
        return marketOverviewRepository.update(value);
    }

    public boolean deleteMarketOverview(Long id) {
        return marketOverviewRepository.delete(id);
    }

    public List<MarketLimitDailySnapshotBO> listLimitDailySnapshots() {
        return marketLimitDailySnapshotRepository.listAll();
    }

    public MarketLimitDailySnapshotBO findLimitDailySnapshotById(Long id) {
        return marketLimitDailySnapshotRepository.findById(id);
    }

    public Long createLimitDailySnapshot(MarketLimitDailySnapshotBO value) {
        return marketLimitDailySnapshotRepository.create(value);
    }

    public boolean updateLimitDailySnapshot(MarketLimitDailySnapshotBO value) {
        return marketLimitDailySnapshotRepository.update(value);
    }

    public boolean deleteLimitDailySnapshot(Long id) {
        return marketLimitDailySnapshotRepository.delete(id);
    }

    public List<MarketLimitDailyStockBO> listLimitDailyStocks() {
        return marketLimitDailyStockRepository.listAll();
    }

    public MarketLimitDailyStockBO findLimitDailyStockById(Long id) {
        return marketLimitDailyStockRepository.findById(id);
    }

    public Long createLimitDailyStock(MarketLimitDailyStockBO value) {
        return marketLimitDailyStockRepository.create(value);
    }

    public boolean updateLimitDailyStock(MarketLimitDailyStockBO value) {
        return marketLimitDailyStockRepository.update(value);
    }

    public boolean deleteLimitDailyStock(Long id) {
        return marketLimitDailyStockRepository.delete(id);
    }

    public List<MarketPulseCaptureBatchBO> listPulseCaptureBatches() {
        return marketPulseCaptureBatchRepository.listAll();
    }

    public MarketPulseCaptureBatchBO findPulseCaptureBatchById(Long id) {
        return marketPulseCaptureBatchRepository.findById(id);
    }

    public Long createPulseCaptureBatch(MarketPulseCaptureBatchBO value) {
        return marketPulseCaptureBatchRepository.create(value);
    }

    public boolean updatePulseCaptureBatch(MarketPulseCaptureBatchBO value) {
        return marketPulseCaptureBatchRepository.update(value);
    }

    public boolean deletePulseCaptureBatch(Long id) {
        return marketPulseCaptureBatchRepository.delete(id);
    }

    public List<MarketPulseSectorDailySnapshotBO> listPulseSectorDailySnapshots() {
        return marketPulseSectorDailySnapshotRepository.listAll();
    }

    public MarketPulseSectorDailySnapshotBO findPulseSectorDailySnapshotById(Long id) {
        return marketPulseSectorDailySnapshotRepository.findById(id);
    }

    public Long createPulseSectorDailySnapshot(MarketPulseSectorDailySnapshotBO value) {
        return marketPulseSectorDailySnapshotRepository.create(value);
    }

    public boolean updatePulseSectorDailySnapshot(MarketPulseSectorDailySnapshotBO value) {
        return marketPulseSectorDailySnapshotRepository.update(value);
    }

    public boolean deletePulseSectorDailySnapshot(Long id) {
        return marketPulseSectorDailySnapshotRepository.delete(id);
    }

    public List<SectorFundFlowCaptureBatchBO> listSectorFundFlowCaptureBatches() {
        return sectorFundFlowCaptureBatchRepository.listAll();
    }

    public SectorFundFlowCaptureBatchBO findSectorFundFlowCaptureBatchById(Long id) {
        return sectorFundFlowCaptureBatchRepository.findById(id);
    }

    public Long createSectorFundFlowCaptureBatch(SectorFundFlowCaptureBatchBO value) {
        return sectorFundFlowCaptureBatchRepository.create(value);
    }

    public boolean updateSectorFundFlowCaptureBatch(SectorFundFlowCaptureBatchBO value) {
        return sectorFundFlowCaptureBatchRepository.update(value);
    }

    public boolean deleteSectorFundFlowCaptureBatch(Long id) {
        return sectorFundFlowCaptureBatchRepository.delete(id);
    }

    public List<SectorFundFlowDailySnapshotBO> listSectorFundFlowDailySnapshots() {
        return sectorFundFlowDailySnapshotRepository.listAll();
    }

    public SectorFundFlowDailySnapshotBO findSectorFundFlowDailySnapshotById(Long id) {
        return sectorFundFlowDailySnapshotRepository.findById(id);
    }

    public Long createSectorFundFlowDailySnapshot(SectorFundFlowDailySnapshotBO value) {
        return sectorFundFlowDailySnapshotRepository.create(value);
    }

    public boolean updateSectorFundFlowDailySnapshot(SectorFundFlowDailySnapshotBO value) {
        return sectorFundFlowDailySnapshotRepository.update(value);
    }

    public boolean deleteSectorFundFlowDailySnapshot(Long id) {
        return sectorFundFlowDailySnapshotRepository.delete(id);
    }

    public List<SelfSelectedListBO> listSelfSelectedLists() {
        return selfSelectedListRepository.listAll();
    }

    public SelfSelectedListBO findSelfSelectedListById(Long id) {
        return selfSelectedListRepository.findById(id);
    }

    public Long createSelfSelectedList(SelfSelectedListBO value) {
        return selfSelectedListRepository.create(value);
    }

    public boolean updateSelfSelectedList(SelfSelectedListBO value) {
        return selfSelectedListRepository.update(value);
    }

    public boolean deleteSelfSelectedList(Long id) {
        return selfSelectedListRepository.delete(id);
    }

    public List<SelfSelectedListItemBO> listSelfSelectedListItems() {
        return selfSelectedListItemRepository.listAll();
    }

    public SelfSelectedListItemBO findSelfSelectedListItemById(Long id) {
        return selfSelectedListItemRepository.findById(id);
    }

    public Long createSelfSelectedListItem(SelfSelectedListItemBO value) {
        return selfSelectedListItemRepository.create(value);
    }

    public boolean updateSelfSelectedListItem(SelfSelectedListItemBO value) {
        return selfSelectedListItemRepository.update(value);
    }

    public boolean deleteSelfSelectedListItem(Long id) {
        return selfSelectedListItemRepository.delete(id);
    }
}
