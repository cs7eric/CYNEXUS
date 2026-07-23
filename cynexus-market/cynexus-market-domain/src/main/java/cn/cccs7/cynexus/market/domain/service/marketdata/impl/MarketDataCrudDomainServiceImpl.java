package cn.cccs7.cynexus.market.domain.service.marketdata.impl;

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
import cn.cccs7.cynexus.market.domain.service.marketdata.MarketDataCrudDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarketDataCrudDomainServiceImpl implements MarketDataCrudDomainService {

    private final MarketOverviewRepository marketOverviewRepository;
    private final MarketLimitDailySnapshotRepository marketLimitDailySnapshotRepository;
    private final MarketLimitDailyStockRepository marketLimitDailyStockRepository;
    private final MarketPulseCaptureBatchRepository marketPulseCaptureBatchRepository;
    private final MarketPulseSectorDailySnapshotRepository marketPulseSectorDailySnapshotRepository;
    private final SectorFundFlowCaptureBatchRepository sectorFundFlowCaptureBatchRepository;
    private final SectorFundFlowDailySnapshotRepository sectorFundFlowDailySnapshotRepository;
    private final SelfSelectedListRepository selfSelectedListRepository;
    private final SelfSelectedListItemRepository selfSelectedListItemRepository;

    public MarketDataCrudDomainServiceImpl(
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

    @Override
    public List<MarketOverviewBO> listMarketOverviews() {
        return marketOverviewRepository.listAll();
    }

    @Override
    public MarketOverviewBO findMarketOverviewById(Long id) {
        return marketOverviewRepository.findById(id);
    }

    @Override
    public Long createMarketOverview(MarketOverviewBO value) {
        return marketOverviewRepository.create(value);
    }

    @Override
    public boolean updateMarketOverview(MarketOverviewBO value) {
        return marketOverviewRepository.update(value);
    }

    @Override
    public boolean deleteMarketOverview(Long id) {
        return marketOverviewRepository.delete(id);
    }

    @Override
    public List<MarketLimitDailySnapshotBO> listLimitDailySnapshots() {
        return marketLimitDailySnapshotRepository.listAll();
    }

    @Override
    public MarketLimitDailySnapshotBO findLimitDailySnapshotById(Long id) {
        return marketLimitDailySnapshotRepository.findById(id);
    }

    @Override
    public Long createLimitDailySnapshot(MarketLimitDailySnapshotBO value) {
        return marketLimitDailySnapshotRepository.create(value);
    }

    @Override
    public boolean updateLimitDailySnapshot(MarketLimitDailySnapshotBO value) {
        return marketLimitDailySnapshotRepository.update(value);
    }

    @Override
    public boolean deleteLimitDailySnapshot(Long id) {
        return marketLimitDailySnapshotRepository.delete(id);
    }

    @Override
    public List<MarketLimitDailyStockBO> listLimitDailyStocks() {
        return marketLimitDailyStockRepository.listAll();
    }

    @Override
    public MarketLimitDailyStockBO findLimitDailyStockById(Long id) {
        return marketLimitDailyStockRepository.findById(id);
    }

    @Override
    public Long createLimitDailyStock(MarketLimitDailyStockBO value) {
        return marketLimitDailyStockRepository.create(value);
    }

    @Override
    public boolean updateLimitDailyStock(MarketLimitDailyStockBO value) {
        return marketLimitDailyStockRepository.update(value);
    }

    @Override
    public boolean deleteLimitDailyStock(Long id) {
        return marketLimitDailyStockRepository.delete(id);
    }

    @Override
    public List<MarketPulseCaptureBatchBO> listPulseCaptureBatches() {
        return marketPulseCaptureBatchRepository.listAll();
    }

    @Override
    public MarketPulseCaptureBatchBO findPulseCaptureBatchById(Long id) {
        return marketPulseCaptureBatchRepository.findById(id);
    }

    @Override
    public Long createPulseCaptureBatch(MarketPulseCaptureBatchBO value) {
        return marketPulseCaptureBatchRepository.create(value);
    }

    @Override
    public boolean updatePulseCaptureBatch(MarketPulseCaptureBatchBO value) {
        return marketPulseCaptureBatchRepository.update(value);
    }

    @Override
    public boolean deletePulseCaptureBatch(Long id) {
        return marketPulseCaptureBatchRepository.delete(id);
    }

    @Override
    public List<MarketPulseSectorDailySnapshotBO> listPulseSectorDailySnapshots() {
        return marketPulseSectorDailySnapshotRepository.listAll();
    }

    @Override
    public MarketPulseSectorDailySnapshotBO findPulseSectorDailySnapshotById(Long id) {
        return marketPulseSectorDailySnapshotRepository.findById(id);
    }

    @Override
    public Long createPulseSectorDailySnapshot(MarketPulseSectorDailySnapshotBO value) {
        return marketPulseSectorDailySnapshotRepository.create(value);
    }

    @Override
    public boolean updatePulseSectorDailySnapshot(MarketPulseSectorDailySnapshotBO value) {
        return marketPulseSectorDailySnapshotRepository.update(value);
    }

    @Override
    public boolean deletePulseSectorDailySnapshot(Long id) {
        return marketPulseSectorDailySnapshotRepository.delete(id);
    }

    @Override
    public List<SectorFundFlowCaptureBatchBO> listSectorFundFlowCaptureBatches() {
        return sectorFundFlowCaptureBatchRepository.listAll();
    }

    @Override
    public SectorFundFlowCaptureBatchBO findSectorFundFlowCaptureBatchById(Long id) {
        return sectorFundFlowCaptureBatchRepository.findById(id);
    }

    @Override
    public Long createSectorFundFlowCaptureBatch(SectorFundFlowCaptureBatchBO value) {
        return sectorFundFlowCaptureBatchRepository.create(value);
    }

    @Override
    public boolean updateSectorFundFlowCaptureBatch(SectorFundFlowCaptureBatchBO value) {
        return sectorFundFlowCaptureBatchRepository.update(value);
    }

    @Override
    public boolean deleteSectorFundFlowCaptureBatch(Long id) {
        return sectorFundFlowCaptureBatchRepository.delete(id);
    }

    @Override
    public List<SectorFundFlowDailySnapshotBO> listSectorFundFlowDailySnapshots() {
        return sectorFundFlowDailySnapshotRepository.listAll();
    }

    @Override
    public SectorFundFlowDailySnapshotBO findSectorFundFlowDailySnapshotById(Long id) {
        return sectorFundFlowDailySnapshotRepository.findById(id);
    }

    @Override
    public Long createSectorFundFlowDailySnapshot(SectorFundFlowDailySnapshotBO value) {
        return sectorFundFlowDailySnapshotRepository.create(value);
    }

    @Override
    public boolean updateSectorFundFlowDailySnapshot(SectorFundFlowDailySnapshotBO value) {
        return sectorFundFlowDailySnapshotRepository.update(value);
    }

    @Override
    public boolean deleteSectorFundFlowDailySnapshot(Long id) {
        return sectorFundFlowDailySnapshotRepository.delete(id);
    }

    @Override
    public List<SelfSelectedListBO> listSelfSelectedLists() {
        return selfSelectedListRepository.listAll();
    }

    @Override
    public SelfSelectedListBO findSelfSelectedListById(Long id) {
        return selfSelectedListRepository.findById(id);
    }

    @Override
    public Long createSelfSelectedList(SelfSelectedListBO value) {
        return selfSelectedListRepository.create(value);
    }

    @Override
    public boolean updateSelfSelectedList(SelfSelectedListBO value) {
        return selfSelectedListRepository.update(value);
    }

    @Override
    public boolean deleteSelfSelectedList(Long id) {
        return selfSelectedListRepository.delete(id);
    }

    @Override
    public List<SelfSelectedListItemBO> listSelfSelectedListItems() {
        return selfSelectedListItemRepository.listAll();
    }

    @Override
    public SelfSelectedListItemBO findSelfSelectedListItemById(Long id) {
        return selfSelectedListItemRepository.findById(id);
    }

    @Override
    public Long createSelfSelectedListItem(SelfSelectedListItemBO value) {
        return selfSelectedListItemRepository.create(value);
    }

    @Override
    public boolean updateSelfSelectedListItem(SelfSelectedListItemBO value) {
        return selfSelectedListItemRepository.update(value);
    }

    @Override
    public boolean deleteSelfSelectedListItem(Long id) {
        return selfSelectedListItemRepository.delete(id);
    }
}
