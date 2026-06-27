package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.StyleRiskDailyDTOConverter;
import cn.cccs7.cynexus.market.api.dto.StyleRiskDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.StyleRiskDailyBO;
import cn.cccs7.cynexus.market.domain.service.StyleRiskDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StyleRiskDailyCrudAppService {

    private final StyleRiskDailyDomainService styleRiskDailyDomainService;

    public StyleRiskDailyCrudAppService(StyleRiskDailyDomainService styleRiskDailyDomainService) {
        this.styleRiskDailyDomainService = styleRiskDailyDomainService;
    }

    public List<StyleRiskDailyDTO> list() {
        return StyleRiskDailyDTOConverter.INSTANCE.toSourceList(styleRiskDailyDomainService.list());
    }

    public StyleRiskDailyDTO findById(Long id) {
        StyleRiskDailyBO value = styleRiskDailyDomainService.findById(id);
        return value == null ? null : StyleRiskDailyDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(StyleRiskDailyDTO dto) {
        return styleRiskDailyDomainService.create(StyleRiskDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(StyleRiskDailyDTO dto) {
        return styleRiskDailyDomainService.update(StyleRiskDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return styleRiskDailyDomainService.delete(id);
    }
}
