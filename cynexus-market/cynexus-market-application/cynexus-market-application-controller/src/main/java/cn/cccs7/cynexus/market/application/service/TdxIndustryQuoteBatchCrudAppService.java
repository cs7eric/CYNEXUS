package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.TdxIndustryQuoteBatchDTOConverter;
import cn.cccs7.cynexus.market.api.dto.TdxIndustryQuoteBatchDTO;
import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteBatchBO;
import cn.cccs7.cynexus.market.domain.service.TdxIndustryQuoteBatchDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TdxIndustryQuoteBatchCrudAppService {

    private final TdxIndustryQuoteBatchDomainService tdxIndustryQuoteBatchDomainService;

    public TdxIndustryQuoteBatchCrudAppService(TdxIndustryQuoteBatchDomainService tdxIndustryQuoteBatchDomainService) {
        this.tdxIndustryQuoteBatchDomainService = tdxIndustryQuoteBatchDomainService;
    }

    public List<TdxIndustryQuoteBatchDTO> list() {
        return TdxIndustryQuoteBatchDTOConverter.INSTANCE.toSourceList(tdxIndustryQuoteBatchDomainService.list());
    }

    public TdxIndustryQuoteBatchDTO findById(Long id) {
        TdxIndustryQuoteBatchBO value = tdxIndustryQuoteBatchDomainService.findById(id);
        return value == null ? null : TdxIndustryQuoteBatchDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(TdxIndustryQuoteBatchDTO dto) {
        return tdxIndustryQuoteBatchDomainService.create(TdxIndustryQuoteBatchDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(TdxIndustryQuoteBatchDTO dto) {
        return tdxIndustryQuoteBatchDomainService.update(TdxIndustryQuoteBatchDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return tdxIndustryQuoteBatchDomainService.delete(id);
    }
}
