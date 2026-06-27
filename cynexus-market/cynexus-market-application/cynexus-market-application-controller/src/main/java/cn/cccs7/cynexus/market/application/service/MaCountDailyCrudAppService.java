package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.MaCountDailyDTOConverter;
import cn.cccs7.cynexus.market.api.dto.MaCountDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.MaCountDailyBO;
import cn.cccs7.cynexus.market.domain.service.MaCountDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MaCountDailyCrudAppService {

    private final MaCountDailyDomainService maCountDailyDomainService;

    public MaCountDailyCrudAppService(MaCountDailyDomainService maCountDailyDomainService) {
        this.maCountDailyDomainService = maCountDailyDomainService;
    }

    public List<MaCountDailyDTO> list() {
        return MaCountDailyDTOConverter.INSTANCE.toSourceList(maCountDailyDomainService.list());
    }

    public MaCountDailyDTO findById(Long id) {
        MaCountDailyBO value = maCountDailyDomainService.findById(id);
        return value == null ? null : MaCountDailyDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(MaCountDailyDTO dto) {
        return maCountDailyDomainService.create(MaCountDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(MaCountDailyDTO dto) {
        return maCountDailyDomainService.update(MaCountDailyDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return maCountDailyDomainService.delete(id);
    }
}
