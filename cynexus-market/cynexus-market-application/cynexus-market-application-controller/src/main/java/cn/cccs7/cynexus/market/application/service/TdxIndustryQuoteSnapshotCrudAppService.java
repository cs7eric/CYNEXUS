package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.TdxIndustryQuoteSnapshotDTOConverter;
import cn.cccs7.cynexus.market.api.dto.TdxIndustryQuoteSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteSnapshotBO;
import cn.cccs7.cynexus.market.domain.sector.TdxIndustryQuoteSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TdxIndustryQuoteSnapshotCrudAppService {

    private final TdxIndustryQuoteSnapshotDomainService tdxIndustryQuoteSnapshotDomainService;

    public TdxIndustryQuoteSnapshotCrudAppService(TdxIndustryQuoteSnapshotDomainService tdxIndustryQuoteSnapshotDomainService) {
        this.tdxIndustryQuoteSnapshotDomainService = tdxIndustryQuoteSnapshotDomainService;
    }

    public List<TdxIndustryQuoteSnapshotDTO> list() {
        return TdxIndustryQuoteSnapshotDTOConverter.INSTANCE.toSourceList(tdxIndustryQuoteSnapshotDomainService.list());
    }

    public TdxIndustryQuoteSnapshotDTO findById(Long id) {
        TdxIndustryQuoteSnapshotBO value = tdxIndustryQuoteSnapshotDomainService.findById(id);
        return value == null ? null : TdxIndustryQuoteSnapshotDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(TdxIndustryQuoteSnapshotDTO dto) {
        return tdxIndustryQuoteSnapshotDomainService.create(TdxIndustryQuoteSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(TdxIndustryQuoteSnapshotDTO dto) {
        return tdxIndustryQuoteSnapshotDomainService.update(TdxIndustryQuoteSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return tdxIndustryQuoteSnapshotDomainService.delete(id);
    }
}
