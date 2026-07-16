package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.StockSectorDTOConverter;
import cn.cccs7.cynexus.market.api.dto.StockSectorDTO;
import cn.cccs7.cynexus.market.domain.entity.StockSectorBO;
import cn.cccs7.cynexus.market.domain.stock.StockSectorDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockSectorCrudAppService {

    private final StockSectorDomainService stockSectorDomainService;

    public StockSectorCrudAppService(StockSectorDomainService stockSectorDomainService) {
        this.stockSectorDomainService = stockSectorDomainService;
    }

    public List<StockSectorDTO> list() {
        return StockSectorDTOConverter.INSTANCE.toSourceList(stockSectorDomainService.list());
    }

    public StockSectorDTO findById(Long id) {
        StockSectorBO value = stockSectorDomainService.findById(id);
        return value == null ? null : StockSectorDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(StockSectorDTO dto) {
        return stockSectorDomainService.create(StockSectorDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(StockSectorDTO dto) {
        return stockSectorDomainService.update(StockSectorDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return stockSectorDomainService.delete(id);
    }
}
