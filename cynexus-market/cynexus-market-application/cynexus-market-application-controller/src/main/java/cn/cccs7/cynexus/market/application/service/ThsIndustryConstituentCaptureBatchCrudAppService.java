package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.ThsIndustryConstituentCaptureBatchDTOConverter;
import cn.cccs7.cynexus.market.api.dto.ThsIndustryConstituentCaptureBatchDTO;
import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.sector.ThsIndustryConstituentCaptureBatchDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ThsIndustryConstituentCaptureBatchCrudAppService {

    private final ThsIndustryConstituentCaptureBatchDomainService thsIndustryConstituentCaptureBatchDomainService;

    public ThsIndustryConstituentCaptureBatchCrudAppService(ThsIndustryConstituentCaptureBatchDomainService thsIndustryConstituentCaptureBatchDomainService) {
        this.thsIndustryConstituentCaptureBatchDomainService = thsIndustryConstituentCaptureBatchDomainService;
    }

    public List<ThsIndustryConstituentCaptureBatchDTO> list() {
        return ThsIndustryConstituentCaptureBatchDTOConverter.INSTANCE.toSourceList(thsIndustryConstituentCaptureBatchDomainService.list());
    }

    public ThsIndustryConstituentCaptureBatchDTO findById(Long id) {
        ThsIndustryConstituentCaptureBatchBO value = thsIndustryConstituentCaptureBatchDomainService.findById(id);
        return value == null ? null : ThsIndustryConstituentCaptureBatchDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(ThsIndustryConstituentCaptureBatchDTO dto) {
        return thsIndustryConstituentCaptureBatchDomainService.create(ThsIndustryConstituentCaptureBatchDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(ThsIndustryConstituentCaptureBatchDTO dto) {
        return thsIndustryConstituentCaptureBatchDomainService.update(ThsIndustryConstituentCaptureBatchDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return thsIndustryConstituentCaptureBatchDomainService.delete(id);
    }
}
