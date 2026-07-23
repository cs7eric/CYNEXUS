package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.DataSourceFreshnessDTO;
import cn.cccs7.cynexus.market.application.convert.DataSourceFreshnessDTOConverter;
import cn.cccs7.cynexus.market.domain.entity.DataSourceFreshnessBO;
import cn.cccs7.cynexus.market.domain.service.datasource.DataSourceFreshnessDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DataSourceFreshnessAppService {

    private final DataSourceFreshnessDomainService domainService;

    public DataSourceFreshnessAppService(DataSourceFreshnessDomainService domainService) {
        this.domainService = domainService;
    }

    public DataSourceFreshnessDTO snapshot() {
        DataSourceFreshnessBO snapshot = domainService.snapshot();
        List<DataSourceFreshnessDTO.ItemDTO> items = DataSourceFreshnessDTOConverter.INSTANCE.toSourceList(snapshot.getItems());
        return DataSourceFreshnessDTO.builder()
                .ok(true)
                .generatedAt(snapshot.getGeneratedAt())
                .summary(DataSourceFreshnessDTO.SummaryDTO.builder()
                        .total(snapshot.getSummary().getTotal()).fresh(snapshot.getSummary().getFresh())
                        .lagging(snapshot.getSummary().getLagging()).stale(snapshot.getSummary().getStale())
                        .empty(snapshot.getSummary().getEmpty()).untracked(snapshot.getSummary().getUntracked())
                        .error(snapshot.getSummary().getError()).build())
                .items(items)
                .build();
    }
}
