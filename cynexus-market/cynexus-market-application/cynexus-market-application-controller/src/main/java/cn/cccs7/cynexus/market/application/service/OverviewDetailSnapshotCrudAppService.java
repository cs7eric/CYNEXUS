package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.OverviewDetailSnapshotDTOConverter;
import cn.cccs7.cynexus.market.api.dto.OverviewDetailSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.OverviewDetailSnapshotBO;
import cn.cccs7.cynexus.market.domain.service.OverviewDetailSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OverviewDetailSnapshotCrudAppService {

    private final OverviewDetailSnapshotDomainService overviewDetailSnapshotDomainService;

    public OverviewDetailSnapshotCrudAppService(OverviewDetailSnapshotDomainService overviewDetailSnapshotDomainService) {
        this.overviewDetailSnapshotDomainService = overviewDetailSnapshotDomainService;
    }

    public List<OverviewDetailSnapshotDTO> list() {
        return OverviewDetailSnapshotDTOConverter.INSTANCE.toSourceList(overviewDetailSnapshotDomainService.list());
    }

    public OverviewDetailSnapshotDTO findById(Long id) {
        OverviewDetailSnapshotBO value = overviewDetailSnapshotDomainService.findById(id);
        return value == null ? null : OverviewDetailSnapshotDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(OverviewDetailSnapshotDTO dto) {
        return overviewDetailSnapshotDomainService.create(OverviewDetailSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(OverviewDetailSnapshotDTO dto) {
        return overviewDetailSnapshotDomainService.update(OverviewDetailSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return overviewDetailSnapshotDomainService.delete(id);
    }
}
