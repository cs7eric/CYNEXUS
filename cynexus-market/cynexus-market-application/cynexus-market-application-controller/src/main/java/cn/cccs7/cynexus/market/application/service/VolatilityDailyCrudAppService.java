package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.VolatilityDailyDTOConverter;
import cn.cccs7.cynexus.market.api.dto.VolatilityDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.VolatilityDailyBO;
import cn.cccs7.cynexus.market.domain.service.VolatilityDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VolatilityDailyCrudAppService {

    private final VolatilityDailyDomainService volatilityDailyDomainService;

    public VolatilityDailyCrudAppService(VolatilityDailyDomainService volatilityDailyDomainService) {
        this.volatilityDailyDomainService = volatilityDailyDomainService;
    }

    public List<VolatilityDailyDTO> list() {
        return VolatilityDailyDTOConverter.INSTANCE.toSourceList(volatilityDailyDomainService.list());
    }

    public VolatilityDailyDTO findById(Long id) {
        VolatilityDailyBO value = volatilityDailyDomainService.findById(id);
        return value == null ? null : VolatilityDailyDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(VolatilityDailyDTO dto) {
        return volatilityDailyDomainService.create(VolatilityDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(VolatilityDailyDTO dto) {
        return volatilityDailyDomainService.update(VolatilityDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return volatilityDailyDomainService.delete(id);
    }
}
