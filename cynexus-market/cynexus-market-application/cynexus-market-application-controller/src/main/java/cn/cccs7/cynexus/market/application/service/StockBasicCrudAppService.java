package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.StockBasicDTOConverter;
import cn.cccs7.cynexus.market.api.dto.StockBasicDTO;
import cn.cccs7.cynexus.market.domain.entity.StockBasicBO;
import cn.cccs7.cynexus.market.domain.stock.StockBasicDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockBasicCrudAppService {

    private final StockBasicDomainService stockBasicDomainService;

    public StockBasicCrudAppService(StockBasicDomainService stockBasicDomainService) {
        this.stockBasicDomainService = stockBasicDomainService;
    }

    public List<StockBasicDTO> list() {
        return StockBasicDTOConverter.INSTANCE.toSourceList(stockBasicDomainService.list());
    }

    public StockBasicDTO findById(Long id) {
        StockBasicBO value = stockBasicDomainService.findById(id);
        return value == null ? null : StockBasicDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(StockBasicDTO dto) {
        return stockBasicDomainService.create(StockBasicDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(StockBasicDTO dto) {
        return stockBasicDomainService.update(StockBasicDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return stockBasicDomainService.delete(id);
    }
}
