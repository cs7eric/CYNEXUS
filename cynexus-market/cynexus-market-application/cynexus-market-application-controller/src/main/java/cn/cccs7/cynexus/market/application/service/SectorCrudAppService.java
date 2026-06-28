package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.SectorDTOConverter;
import cn.cccs7.cynexus.market.api.dto.SectorDTO;
import cn.cccs7.cynexus.market.domain.entity.SectorBO;
import cn.cccs7.cynexus.market.domain.service.SectorDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SectorCrudAppService {

    private final SectorDomainService sectorDomainService;

    public SectorCrudAppService(SectorDomainService sectorDomainService) {
        this.sectorDomainService = sectorDomainService;
    }

    public List<SectorDTO> list() {
        return SectorDTOConverter.INSTANCE.toSourceList(sectorDomainService.list());
    }

    public SectorDTO findById(Long id) {
        SectorBO value = sectorDomainService.findById(id);
        return value == null ? null : SectorDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(SectorDTO dto) {
        return sectorDomainService.create(SectorDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(SectorDTO dto) {
        return sectorDomainService.update(SectorDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return sectorDomainService.delete(id);
    }
}
