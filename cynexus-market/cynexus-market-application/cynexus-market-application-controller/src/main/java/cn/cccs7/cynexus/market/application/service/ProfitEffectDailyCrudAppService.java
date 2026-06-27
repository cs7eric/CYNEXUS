package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.ProfitEffectDailyDTOConverter;
import cn.cccs7.cynexus.market.api.dto.ProfitEffectDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.ProfitEffectDailyBO;
import cn.cccs7.cynexus.market.domain.service.ProfitEffectDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProfitEffectDailyCrudAppService {

    private final ProfitEffectDailyDomainService profitEffectDailyDomainService;

    public ProfitEffectDailyCrudAppService(ProfitEffectDailyDomainService profitEffectDailyDomainService) {
        this.profitEffectDailyDomainService = profitEffectDailyDomainService;
    }

    public List<ProfitEffectDailyDTO> list() {
        return ProfitEffectDailyDTOConverter.INSTANCE.toSourceList(profitEffectDailyDomainService.list());
    }

    public ProfitEffectDailyDTO findById(Long id) {
        ProfitEffectDailyBO value = profitEffectDailyDomainService.findById(id);
        return value == null ? null : ProfitEffectDailyDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(ProfitEffectDailyDTO dto) {
        return profitEffectDailyDomainService.create(ProfitEffectDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(ProfitEffectDailyDTO dto) {
        return profitEffectDailyDomainService.update(ProfitEffectDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return profitEffectDailyDomainService.delete(id);
    }
}
