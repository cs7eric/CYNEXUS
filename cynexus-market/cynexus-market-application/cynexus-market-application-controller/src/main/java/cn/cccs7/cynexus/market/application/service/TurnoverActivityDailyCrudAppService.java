package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.TurnoverActivityDailyDTOConverter;
import cn.cccs7.cynexus.market.api.dto.TurnoverActivityDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.TurnoverActivityDailyBO;
import cn.cccs7.cynexus.market.domain.service.TurnoverActivityDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TurnoverActivityDailyCrudAppService {

    private final TurnoverActivityDailyDomainService turnoverActivityDailyDomainService;

    public TurnoverActivityDailyCrudAppService(TurnoverActivityDailyDomainService turnoverActivityDailyDomainService) {
        this.turnoverActivityDailyDomainService = turnoverActivityDailyDomainService;
    }

    public List<TurnoverActivityDailyDTO> list() {
        return TurnoverActivityDailyDTOConverter.INSTANCE.toSourceList(turnoverActivityDailyDomainService.list());
    }

    public TurnoverActivityDailyDTO findById(Long id) {
        TurnoverActivityDailyBO value = turnoverActivityDailyDomainService.findById(id);
        return value == null ? null : TurnoverActivityDailyDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(TurnoverActivityDailyDTO dto) {
        return turnoverActivityDailyDomainService.create(TurnoverActivityDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(TurnoverActivityDailyDTO dto) {
        return turnoverActivityDailyDomainService.update(TurnoverActivityDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return turnoverActivityDailyDomainService.delete(id);
    }
}
