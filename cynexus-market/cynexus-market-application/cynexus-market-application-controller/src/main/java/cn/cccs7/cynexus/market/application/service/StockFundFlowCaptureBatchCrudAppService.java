package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.StockFundFlowCaptureBatchDTOConverter;
import cn.cccs7.cynexus.market.api.dto.StockFundFlowCaptureBatchDTO;
import cn.cccs7.cynexus.market.domain.entity.StockFundFlowCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.service.StockFundFlowCaptureBatchDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockFundFlowCaptureBatchCrudAppService {

    private final StockFundFlowCaptureBatchDomainService stockFundFlowCaptureBatchDomainService;

    public StockFundFlowCaptureBatchCrudAppService(StockFundFlowCaptureBatchDomainService stockFundFlowCaptureBatchDomainService) {
        this.stockFundFlowCaptureBatchDomainService = stockFundFlowCaptureBatchDomainService;
    }

    public List<StockFundFlowCaptureBatchDTO> list() {
        return StockFundFlowCaptureBatchDTOConverter.INSTANCE.toSourceList(stockFundFlowCaptureBatchDomainService.list());
    }

    public StockFundFlowCaptureBatchDTO findById(Long id) {
        StockFundFlowCaptureBatchBO value = stockFundFlowCaptureBatchDomainService.findById(id);
        return value == null ? null : StockFundFlowCaptureBatchDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(StockFundFlowCaptureBatchDTO dto) {
        return stockFundFlowCaptureBatchDomainService.create(StockFundFlowCaptureBatchDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(StockFundFlowCaptureBatchDTO dto) {
        return stockFundFlowCaptureBatchDomainService.update(StockFundFlowCaptureBatchDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return stockFundFlowCaptureBatchDomainService.delete(id);
    }
}
