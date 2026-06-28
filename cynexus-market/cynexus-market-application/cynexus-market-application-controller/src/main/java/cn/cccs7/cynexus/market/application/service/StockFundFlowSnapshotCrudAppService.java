package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.StockFundFlowSnapshotDTOConverter;
import cn.cccs7.cynexus.market.api.dto.StockFundFlowSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.StockFundFlowSnapshotBO;
import cn.cccs7.cynexus.market.domain.service.StockFundFlowSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockFundFlowSnapshotCrudAppService {

    private final StockFundFlowSnapshotDomainService stockFundFlowSnapshotDomainService;

    public StockFundFlowSnapshotCrudAppService(StockFundFlowSnapshotDomainService stockFundFlowSnapshotDomainService) {
        this.stockFundFlowSnapshotDomainService = stockFundFlowSnapshotDomainService;
    }

    public List<StockFundFlowSnapshotDTO> list() {
        return StockFundFlowSnapshotDTOConverter.INSTANCE.toSourceList(stockFundFlowSnapshotDomainService.list());
    }

    public StockFundFlowSnapshotDTO findById(Long id) {
        StockFundFlowSnapshotBO value = stockFundFlowSnapshotDomainService.findById(id);
        return value == null ? null : StockFundFlowSnapshotDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(StockFundFlowSnapshotDTO dto) {
        return stockFundFlowSnapshotDomainService.create(StockFundFlowSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(StockFundFlowSnapshotDTO dto) {
        return stockFundFlowSnapshotDomainService.update(StockFundFlowSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return stockFundFlowSnapshotDomainService.delete(id);
    }
}
