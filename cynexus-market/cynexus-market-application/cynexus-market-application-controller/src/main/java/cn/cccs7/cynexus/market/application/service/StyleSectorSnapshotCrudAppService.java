package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.StyleSectorSnapshotDTOConverter;
import cn.cccs7.cynexus.market.api.dto.StyleSectorSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO;
import cn.cccs7.cynexus.market.domain.sector.StyleSectorSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StyleSectorSnapshotCrudAppService {

    private final StyleSectorSnapshotDomainService styleSectorSnapshotDomainService;

    public StyleSectorSnapshotCrudAppService(StyleSectorSnapshotDomainService styleSectorSnapshotDomainService) {
        this.styleSectorSnapshotDomainService = styleSectorSnapshotDomainService;
    }

    public List<StyleSectorSnapshotDTO> list() {
        return StyleSectorSnapshotDTOConverter.INSTANCE.toSourceList(styleSectorSnapshotDomainService.list());
    }

    public StyleSectorSnapshotDTO findById(Long id) {
        StyleSectorSnapshotBO value = styleSectorSnapshotDomainService.findById(id);
        return value == null ? null : StyleSectorSnapshotDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(StyleSectorSnapshotDTO dto) {
        return styleSectorSnapshotDomainService.create(StyleSectorSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(StyleSectorSnapshotDTO dto) {
        return styleSectorSnapshotDomainService.update(StyleSectorSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return styleSectorSnapshotDomainService.delete(id);
    }
}
