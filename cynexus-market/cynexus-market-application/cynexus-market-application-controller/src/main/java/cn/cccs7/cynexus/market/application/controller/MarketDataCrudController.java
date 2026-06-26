package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketLimitDailySnapshotDTO;
import cn.cccs7.cynexus.market.api.dto.MarketLimitDailyStockDTO;
import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseCaptureBatchDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseSectorDailySnapshotDTO;
import cn.cccs7.cynexus.market.api.dto.SectorFundFlowCaptureBatchDTO;
import cn.cccs7.cynexus.market.api.dto.SectorFundFlowDailySnapshotDTO;
import cn.cccs7.cynexus.market.api.dto.SelfSelectedListDTO;
import cn.cccs7.cynexus.market.api.dto.SelfSelectedListItemDTO;
import cn.cccs7.cynexus.market.application.convert.MarketLimitDailySnapshotDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketLimitDailyStockDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketOverviewDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketPulseCaptureBatchDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketPulseSectorDailySnapshotDTOConverter;
import cn.cccs7.cynexus.market.application.convert.SectorFundFlowCaptureBatchDTOConverter;
import cn.cccs7.cynexus.market.application.convert.SectorFundFlowDailySnapshotDTOConverter;
import cn.cccs7.cynexus.market.application.convert.SelfSelectedListDTOConverter;
import cn.cccs7.cynexus.market.application.convert.SelfSelectedListItemDTOConverter;
import cn.cccs7.cynexus.market.application.service.MarketDataCrudAppService;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/data")
public class MarketDataCrudController {

    private final MarketDataCrudAppService appService;

    public MarketDataCrudController(MarketDataCrudAppService appService) {
        this.appService = appService;
    }

    @GetMapping("/overview-snapshots")
    public Result<List<MarketOverviewDTO>> listMarketOverviews() {
        return list(appService::listMarketOverviews, MarketOverviewDTOConverter.INSTANCE);
    }

    @GetMapping("/overview-snapshots/{id}")
    public Result<MarketOverviewDTO> getMarketOverview(@PathVariable Long id) {
        return get(id, appService::findMarketOverviewById, MarketOverviewDTOConverter.INSTANCE);
    }

    @PostMapping("/overview-snapshots")
    public Result<Long> createMarketOverview(@RequestBody MarketOverviewDTO dto) {
        return create(dto, appService::createMarketOverview, MarketOverviewDTOConverter.INSTANCE);
    }

    @PutMapping("/overview-snapshots")
    public Result<Boolean> updateMarketOverview(@RequestBody MarketOverviewDTO dto) {
        return update(dto, appService::updateMarketOverview, MarketOverviewDTOConverter.INSTANCE);
    }

    @DeleteMapping("/overview-snapshots/{id}")
    public Result<Boolean> deleteMarketOverview(@PathVariable Long id) {
        return Result.success(appService.deleteMarketOverview(id));
    }

    @GetMapping("/limit-daily-snapshots")
    public Result<List<MarketLimitDailySnapshotDTO>> listLimitDailySnapshots() {
        return list(appService::listLimitDailySnapshots, MarketLimitDailySnapshotDTOConverter.INSTANCE);
    }

    @GetMapping("/limit-daily-snapshots/{id}")
    public Result<MarketLimitDailySnapshotDTO> getLimitDailySnapshot(@PathVariable Long id) {
        return get(id, appService::findLimitDailySnapshotById, MarketLimitDailySnapshotDTOConverter.INSTANCE);
    }

    @PostMapping("/limit-daily-snapshots")
    public Result<Long> createLimitDailySnapshot(@RequestBody MarketLimitDailySnapshotDTO dto) {
        return create(dto, appService::createLimitDailySnapshot, MarketLimitDailySnapshotDTOConverter.INSTANCE);
    }

    @PutMapping("/limit-daily-snapshots")
    public Result<Boolean> updateLimitDailySnapshot(@RequestBody MarketLimitDailySnapshotDTO dto) {
        return update(dto, appService::updateLimitDailySnapshot, MarketLimitDailySnapshotDTOConverter.INSTANCE);
    }

    @DeleteMapping("/limit-daily-snapshots/{id}")
    public Result<Boolean> deleteLimitDailySnapshot(@PathVariable Long id) {
        return Result.success(appService.deleteLimitDailySnapshot(id));
    }

    @GetMapping("/limit-daily-stocks")
    public Result<List<MarketLimitDailyStockDTO>> listLimitDailyStocks() {
        return list(appService::listLimitDailyStocks, MarketLimitDailyStockDTOConverter.INSTANCE);
    }

    @GetMapping("/limit-daily-stocks/{id}")
    public Result<MarketLimitDailyStockDTO> getLimitDailyStock(@PathVariable Long id) {
        return get(id, appService::findLimitDailyStockById, MarketLimitDailyStockDTOConverter.INSTANCE);
    }

    @PostMapping("/limit-daily-stocks")
    public Result<Long> createLimitDailyStock(@RequestBody MarketLimitDailyStockDTO dto) {
        return create(dto, appService::createLimitDailyStock, MarketLimitDailyStockDTOConverter.INSTANCE);
    }

    @PutMapping("/limit-daily-stocks")
    public Result<Boolean> updateLimitDailyStock(@RequestBody MarketLimitDailyStockDTO dto) {
        return update(dto, appService::updateLimitDailyStock, MarketLimitDailyStockDTOConverter.INSTANCE);
    }

    @DeleteMapping("/limit-daily-stocks/{id}")
    public Result<Boolean> deleteLimitDailyStock(@PathVariable Long id) {
        return Result.success(appService.deleteLimitDailyStock(id));
    }

    @GetMapping("/pulse-capture-batches")
    public Result<List<MarketPulseCaptureBatchDTO>> listPulseCaptureBatches() {
        return list(appService::listPulseCaptureBatches, MarketPulseCaptureBatchDTOConverter.INSTANCE);
    }

    @GetMapping("/pulse-capture-batches/{id}")
    public Result<MarketPulseCaptureBatchDTO> getPulseCaptureBatch(@PathVariable Long id) {
        return get(id, appService::findPulseCaptureBatchById, MarketPulseCaptureBatchDTOConverter.INSTANCE);
    }

    @PostMapping("/pulse-capture-batches")
    public Result<Long> createPulseCaptureBatch(@RequestBody MarketPulseCaptureBatchDTO dto) {
        return create(dto, appService::createPulseCaptureBatch, MarketPulseCaptureBatchDTOConverter.INSTANCE);
    }

    @PutMapping("/pulse-capture-batches")
    public Result<Boolean> updatePulseCaptureBatch(@RequestBody MarketPulseCaptureBatchDTO dto) {
        return update(dto, appService::updatePulseCaptureBatch, MarketPulseCaptureBatchDTOConverter.INSTANCE);
    }

    @DeleteMapping("/pulse-capture-batches/{id}")
    public Result<Boolean> deletePulseCaptureBatch(@PathVariable Long id) {
        return Result.success(appService.deletePulseCaptureBatch(id));
    }

    @GetMapping("/pulse-sector-daily-snapshots")
    public Result<List<MarketPulseSectorDailySnapshotDTO>> listPulseSectorDailySnapshots() {
        return list(appService::listPulseSectorDailySnapshots,
                MarketPulseSectorDailySnapshotDTOConverter.INSTANCE);
    }

    @GetMapping("/pulse-sector-daily-snapshots/{id}")
    public Result<MarketPulseSectorDailySnapshotDTO> getPulseSectorDailySnapshot(@PathVariable Long id) {
        return get(id, appService::findPulseSectorDailySnapshotById,
                MarketPulseSectorDailySnapshotDTOConverter.INSTANCE);
    }

    @PostMapping("/pulse-sector-daily-snapshots")
    public Result<Long> createPulseSectorDailySnapshot(@RequestBody MarketPulseSectorDailySnapshotDTO dto) {
        return create(dto, appService::createPulseSectorDailySnapshot,
                MarketPulseSectorDailySnapshotDTOConverter.INSTANCE);
    }

    @PutMapping("/pulse-sector-daily-snapshots")
    public Result<Boolean> updatePulseSectorDailySnapshot(@RequestBody MarketPulseSectorDailySnapshotDTO dto) {
        return update(dto, appService::updatePulseSectorDailySnapshot,
                MarketPulseSectorDailySnapshotDTOConverter.INSTANCE);
    }

    @DeleteMapping("/pulse-sector-daily-snapshots/{id}")
    public Result<Boolean> deletePulseSectorDailySnapshot(@PathVariable Long id) {
        return Result.success(appService.deletePulseSectorDailySnapshot(id));
    }

    @GetMapping("/sector-fund-flow-capture-batches")
    public Result<List<SectorFundFlowCaptureBatchDTO>> listSectorFundFlowCaptureBatches() {
        return list(appService::listSectorFundFlowCaptureBatches,
                SectorFundFlowCaptureBatchDTOConverter.INSTANCE);
    }

    @GetMapping("/sector-fund-flow-capture-batches/{id}")
    public Result<SectorFundFlowCaptureBatchDTO> getSectorFundFlowCaptureBatch(@PathVariable Long id) {
        return get(id, appService::findSectorFundFlowCaptureBatchById,
                SectorFundFlowCaptureBatchDTOConverter.INSTANCE);
    }

    @PostMapping("/sector-fund-flow-capture-batches")
    public Result<Long> createSectorFundFlowCaptureBatch(@RequestBody SectorFundFlowCaptureBatchDTO dto) {
        return create(dto, appService::createSectorFundFlowCaptureBatch,
                SectorFundFlowCaptureBatchDTOConverter.INSTANCE);
    }

    @PutMapping("/sector-fund-flow-capture-batches")
    public Result<Boolean> updateSectorFundFlowCaptureBatch(@RequestBody SectorFundFlowCaptureBatchDTO dto) {
        return update(dto, appService::updateSectorFundFlowCaptureBatch,
                SectorFundFlowCaptureBatchDTOConverter.INSTANCE);
    }

    @DeleteMapping("/sector-fund-flow-capture-batches/{id}")
    public Result<Boolean> deleteSectorFundFlowCaptureBatch(@PathVariable Long id) {
        return Result.success(appService.deleteSectorFundFlowCaptureBatch(id));
    }

    @GetMapping("/sector-fund-flow-daily-snapshots")
    public Result<List<SectorFundFlowDailySnapshotDTO>> listSectorFundFlowDailySnapshots() {
        return list(appService::listSectorFundFlowDailySnapshots,
                SectorFundFlowDailySnapshotDTOConverter.INSTANCE);
    }

    @GetMapping("/sector-fund-flow-daily-snapshots/{id}")
    public Result<SectorFundFlowDailySnapshotDTO> getSectorFundFlowDailySnapshot(@PathVariable Long id) {
        return get(id, appService::findSectorFundFlowDailySnapshotById,
                SectorFundFlowDailySnapshotDTOConverter.INSTANCE);
    }

    @PostMapping("/sector-fund-flow-daily-snapshots")
    public Result<Long> createSectorFundFlowDailySnapshot(@RequestBody SectorFundFlowDailySnapshotDTO dto) {
        return create(dto, appService::createSectorFundFlowDailySnapshot,
                SectorFundFlowDailySnapshotDTOConverter.INSTANCE);
    }

    @PutMapping("/sector-fund-flow-daily-snapshots")
    public Result<Boolean> updateSectorFundFlowDailySnapshot(@RequestBody SectorFundFlowDailySnapshotDTO dto) {
        return update(dto, appService::updateSectorFundFlowDailySnapshot,
                SectorFundFlowDailySnapshotDTOConverter.INSTANCE);
    }

    @DeleteMapping("/sector-fund-flow-daily-snapshots/{id}")
    public Result<Boolean> deleteSectorFundFlowDailySnapshot(@PathVariable Long id) {
        return Result.success(appService.deleteSectorFundFlowDailySnapshot(id));
    }

    @GetMapping("/self-selected-lists")
    public Result<List<SelfSelectedListDTO>> listSelfSelectedLists() {
        return list(appService::listSelfSelectedLists, SelfSelectedListDTOConverter.INSTANCE);
    }

    @GetMapping("/self-selected-lists/{id}")
    public Result<SelfSelectedListDTO> getSelfSelectedList(@PathVariable Long id) {
        return get(id, appService::findSelfSelectedListById, SelfSelectedListDTOConverter.INSTANCE);
    }

    @PostMapping("/self-selected-lists")
    public Result<Long> createSelfSelectedList(@RequestBody SelfSelectedListDTO dto) {
        return create(dto, appService::createSelfSelectedList, SelfSelectedListDTOConverter.INSTANCE);
    }

    @PutMapping("/self-selected-lists")
    public Result<Boolean> updateSelfSelectedList(@RequestBody SelfSelectedListDTO dto) {
        return update(dto, appService::updateSelfSelectedList, SelfSelectedListDTOConverter.INSTANCE);
    }

    @DeleteMapping("/self-selected-lists/{id}")
    public Result<Boolean> deleteSelfSelectedList(@PathVariable Long id) {
        return Result.success(appService.deleteSelfSelectedList(id));
    }

    @GetMapping("/self-selected-list-items")
    public Result<List<SelfSelectedListItemDTO>> listSelfSelectedListItems() {
        return list(appService::listSelfSelectedListItems, SelfSelectedListItemDTOConverter.INSTANCE);
    }

    @GetMapping("/self-selected-list-items/{id}")
    public Result<SelfSelectedListItemDTO> getSelfSelectedListItem(@PathVariable Long id) {
        return get(id, appService::findSelfSelectedListItemById, SelfSelectedListItemDTOConverter.INSTANCE);
    }

    @PostMapping("/self-selected-list-items")
    public Result<Long> createSelfSelectedListItem(@RequestBody SelfSelectedListItemDTO dto) {
        return create(dto, appService::createSelfSelectedListItem, SelfSelectedListItemDTOConverter.INSTANCE);
    }

    @PutMapping("/self-selected-list-items")
    public Result<Boolean> updateSelfSelectedListItem(@RequestBody SelfSelectedListItemDTO dto) {
        return update(dto, appService::updateSelfSelectedListItem, SelfSelectedListItemDTOConverter.INSTANCE);
    }

    @DeleteMapping("/self-selected-list-items/{id}")
    public Result<Boolean> deleteSelfSelectedListItem(@PathVariable Long id) {
        return Result.success(appService.deleteSelfSelectedListItem(id));
    }

    private <D, B> Result<List<D>> list(Supplier<List<B>> supplier, BaseMapper<D, B> converter) {
        return Result.success(converter.toSourceList(supplier.get()));
    }

    private <D, B> Result<D> get(Long id, Function<Long, B> finder, BaseMapper<D, B> converter) {
        B value = finder.apply(id);
        return Result.success(value == null ? null : converter.toSource(value));
    }

    private <D, B> Result<Long> create(D dto, Function<B, Long> creator, BaseMapper<D, B> converter) {
        return Result.success(creator.apply(converter.toTarget(dto)));
    }

    private <D, B> Result<Boolean> update(D dto, Function<B, Boolean> updater, BaseMapper<D, B> converter) {
        return Result.success(updater.apply(converter.toTarget(dto)));
    }
}
