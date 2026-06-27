package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.SectorBreadthDailyDTOConverter;
import cn.cccs7.cynexus.market.api.dto.SectorBreadthDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.SectorBreadthDailyBO;
import cn.cccs7.cynexus.market.domain.service.SectorBreadthDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SectorBreadthDailyCrudAppService {

    private final SectorBreadthDailyDomainService sectorBreadthDailyDomainService;

    public SectorBreadthDailyCrudAppService(SectorBreadthDailyDomainService sectorBreadthDailyDomainService) {
        this.sectorBreadthDailyDomainService = sectorBreadthDailyDomainService;
    }

    public List<SectorBreadthDailyDTO> list() {
        return SectorBreadthDailyDTOConverter.INSTANCE.toSourceList(sectorBreadthDailyDomainService.list());
    }

    public SectorBreadthDailyDTO findById(Long id) {
        SectorBreadthDailyBO value = sectorBreadthDailyDomainService.findById(id);
        return value == null ? null : SectorBreadthDailyDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(SectorBreadthDailyDTO dto) {
        return sectorBreadthDailyDomainService.create(SectorBreadthDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(SectorBreadthDailyDTO dto) {
        return sectorBreadthDailyDomainService.update(SectorBreadthDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return sectorBreadthDailyDomainService.delete(id);
    }
}
