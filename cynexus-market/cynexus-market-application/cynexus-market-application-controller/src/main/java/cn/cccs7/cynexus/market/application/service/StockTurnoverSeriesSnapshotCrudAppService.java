package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.StockTurnoverSeriesSnapshotDTOConverter;
import cn.cccs7.cynexus.market.api.dto.StockTurnoverSeriesSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.StockTurnoverSeriesSnapshotBO;
import cn.cccs7.cynexus.market.domain.stock.StockTurnoverSeriesSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockTurnoverSeriesSnapshotCrudAppService {

    private final StockTurnoverSeriesSnapshotDomainService stockTurnoverSeriesSnapshotDomainService;

    public StockTurnoverSeriesSnapshotCrudAppService(StockTurnoverSeriesSnapshotDomainService stockTurnoverSeriesSnapshotDomainService) {
        this.stockTurnoverSeriesSnapshotDomainService = stockTurnoverSeriesSnapshotDomainService;
    }

    public List<StockTurnoverSeriesSnapshotDTO> list() {
        return StockTurnoverSeriesSnapshotDTOConverter.INSTANCE.toSourceList(stockTurnoverSeriesSnapshotDomainService.list());
    }

    public StockTurnoverSeriesSnapshotDTO findById(Long id) {
        StockTurnoverSeriesSnapshotBO value = stockTurnoverSeriesSnapshotDomainService.findById(id);
        return value == null ? null : StockTurnoverSeriesSnapshotDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(StockTurnoverSeriesSnapshotDTO dto) {
        return stockTurnoverSeriesSnapshotDomainService.create(StockTurnoverSeriesSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(StockTurnoverSeriesSnapshotDTO dto) {
        return stockTurnoverSeriesSnapshotDomainService.update(StockTurnoverSeriesSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return stockTurnoverSeriesSnapshotDomainService.delete(id);
    }
}
