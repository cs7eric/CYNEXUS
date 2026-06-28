package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.application.convert.TdxSectorQuoteDetailSnapshotDTOConverter;
import cn.cccs7.cynexus.market.api.dto.TdxSectorQuoteDetailSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.TdxSectorQuoteDetailSnapshotBO;
import cn.cccs7.cynexus.market.domain.service.TdxSectorQuoteDetailSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TdxSectorQuoteDetailSnapshotCrudAppService {

    private final TdxSectorQuoteDetailSnapshotDomainService tdxSectorQuoteDetailSnapshotDomainService;

    public TdxSectorQuoteDetailSnapshotCrudAppService(TdxSectorQuoteDetailSnapshotDomainService tdxSectorQuoteDetailSnapshotDomainService) {
        this.tdxSectorQuoteDetailSnapshotDomainService = tdxSectorQuoteDetailSnapshotDomainService;
    }

    public List<TdxSectorQuoteDetailSnapshotDTO> list() {
        return TdxSectorQuoteDetailSnapshotDTOConverter.INSTANCE.toSourceList(tdxSectorQuoteDetailSnapshotDomainService.list());
    }

    public TdxSectorQuoteDetailSnapshotDTO findById(Long id) {
        TdxSectorQuoteDetailSnapshotBO value = tdxSectorQuoteDetailSnapshotDomainService.findById(id);
        return value == null ? null : TdxSectorQuoteDetailSnapshotDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(TdxSectorQuoteDetailSnapshotDTO dto) {
        return tdxSectorQuoteDetailSnapshotDomainService.create(TdxSectorQuoteDetailSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(TdxSectorQuoteDetailSnapshotDTO dto) {
        return tdxSectorQuoteDetailSnapshotDomainService.update(TdxSectorQuoteDetailSnapshotDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return tdxSectorQuoteDetailSnapshotDomainService.delete(id);
    }
}
