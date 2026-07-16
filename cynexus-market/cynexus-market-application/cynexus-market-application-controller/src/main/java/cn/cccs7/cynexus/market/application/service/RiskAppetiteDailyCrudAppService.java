package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.RiskAppetiteDailyDTOConverter;
import cn.cccs7.cynexus.market.api.dto.RiskAppetiteDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.RiskAppetiteDailyBO;
import cn.cccs7.cynexus.market.domain.sentiment.RiskAppetiteDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RiskAppetiteDailyCrudAppService {

    private final RiskAppetiteDailyDomainService riskAppetiteDailyDomainService;

    public RiskAppetiteDailyCrudAppService(RiskAppetiteDailyDomainService riskAppetiteDailyDomainService) {
        this.riskAppetiteDailyDomainService = riskAppetiteDailyDomainService;
    }

    public List<RiskAppetiteDailyDTO> list() {
        return RiskAppetiteDailyDTOConverter.INSTANCE.toSourceList(riskAppetiteDailyDomainService.list());
    }

    public RiskAppetiteDailyDTO findById(Long id) {
        RiskAppetiteDailyBO value = riskAppetiteDailyDomainService.findById(id);
        return value == null ? null : RiskAppetiteDailyDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(RiskAppetiteDailyDTO dto) {
        return riskAppetiteDailyDomainService.create(RiskAppetiteDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(RiskAppetiteDailyDTO dto) {
        return riskAppetiteDailyDomainService.update(RiskAppetiteDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return riskAppetiteDailyDomainService.delete(id);
    }
}
