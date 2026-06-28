package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.TradeCalendarDTOConverter;
import cn.cccs7.cynexus.market.api.dto.TradeCalendarDTO;
import cn.cccs7.cynexus.market.domain.entity.TradeCalendarBO;
import cn.cccs7.cynexus.market.domain.service.TradeCalendarDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TradeCalendarCrudAppService {

    private final TradeCalendarDomainService tradeCalendarDomainService;

    public TradeCalendarCrudAppService(TradeCalendarDomainService tradeCalendarDomainService) {
        this.tradeCalendarDomainService = tradeCalendarDomainService;
    }

    public List<TradeCalendarDTO> list() {
        return TradeCalendarDTOConverter.INSTANCE.toSourceList(tradeCalendarDomainService.list());
    }

    public TradeCalendarDTO findById(Long id) {
        TradeCalendarBO value = tradeCalendarDomainService.findById(id);
        return value == null ? null : TradeCalendarDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(TradeCalendarDTO dto) {
        return tradeCalendarDomainService.create(TradeCalendarDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(TradeCalendarDTO dto) {
        return tradeCalendarDomainService.update(TradeCalendarDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return tradeCalendarDomainService.delete(id);
    }
}
