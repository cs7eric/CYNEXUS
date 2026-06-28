package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.ThsIndustryConstituentSnapshotDTOConverter;
import cn.cccs7.cynexus.market.api.dto.ThsIndustryConstituentSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentSnapshotBO;
import cn.cccs7.cynexus.market.domain.service.ThsIndustryConstituentSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ThsIndustryConstituentSnapshotCrudAppService {

    private final ThsIndustryConstituentSnapshotDomainService thsIndustryConstituentSnapshotDomainService;

    public ThsIndustryConstituentSnapshotCrudAppService(ThsIndustryConstituentSnapshotDomainService thsIndustryConstituentSnapshotDomainService) {
        this.thsIndustryConstituentSnapshotDomainService = thsIndustryConstituentSnapshotDomainService;
    }

    public List<ThsIndustryConstituentSnapshotDTO> list() {
        return ThsIndustryConstituentSnapshotDTOConverter.INSTANCE.toSourceList(thsIndustryConstituentSnapshotDomainService.list());
    }

    public ThsIndustryConstituentSnapshotDTO findById(Long id) {
        ThsIndustryConstituentSnapshotBO value = thsIndustryConstituentSnapshotDomainService.findById(id);
        return value == null ? null : ThsIndustryConstituentSnapshotDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(ThsIndustryConstituentSnapshotDTO dto) {
        return thsIndustryConstituentSnapshotDomainService.create(ThsIndustryConstituentSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(ThsIndustryConstituentSnapshotDTO dto) {
        return thsIndustryConstituentSnapshotDomainService.update(ThsIndustryConstituentSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return thsIndustryConstituentSnapshotDomainService.delete(id);
    }
}
