package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.HeatmapSnapshotDTOConverter;
import cn.cccs7.cynexus.market.api.dto.HeatmapSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.HeatmapSnapshotBO;
import cn.cccs7.cynexus.market.domain.pulse.HeatmapSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HeatmapSnapshotCrudAppService {

    private final HeatmapSnapshotDomainService heatmapSnapshotDomainService;

    public HeatmapSnapshotCrudAppService(HeatmapSnapshotDomainService heatmapSnapshotDomainService) {
        this.heatmapSnapshotDomainService = heatmapSnapshotDomainService;
    }

    public List<HeatmapSnapshotDTO> list() {
        return HeatmapSnapshotDTOConverter.INSTANCE.toSourceList(heatmapSnapshotDomainService.list());
    }

    public HeatmapSnapshotDTO findById(Long id) {
        HeatmapSnapshotBO value = heatmapSnapshotDomainService.findById(id);
        return value == null ? null : HeatmapSnapshotDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(HeatmapSnapshotDTO dto) {
        return heatmapSnapshotDomainService.create(HeatmapSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(HeatmapSnapshotDTO dto) {
        return heatmapSnapshotDomainService.update(HeatmapSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return heatmapSnapshotDomainService.delete(id);
    }
}
