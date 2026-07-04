package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.DataSourceFreshnessDTO;
import cn.cccs7.cynexus.market.application.convert.DataSourceFreshnessDTOConverter;
import cn.cccs7.cynexus.market.domain.entity.DataSourceFreshnessBO;
import cn.cccs7.cynexus.market.domain.repository.DataSourceFreshnessRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DataSourceFreshnessAppService {

    private final DataSourceFreshnessRepository dataSourceFreshnessRepository;

    public DataSourceFreshnessAppService(DataSourceFreshnessRepository dataSourceFreshnessRepository) {
        this.dataSourceFreshnessRepository = dataSourceFreshnessRepository;
    }

    public DataSourceFreshnessDTO snapshot() {
        DataSourceFreshnessBO snapshot = dataSourceFreshnessRepository.snapshot();
        List<DataSourceFreshnessDTO.ItemDTO> items = DataSourceFreshnessDTOConverter.INSTANCE.toSourceList(snapshot.getItems());
        return DataSourceFreshnessDTO.builder()
                .ok(true)
                .generatedAt(snapshot.getGeneratedAt())
                .summary(summary(items))
                .items(items)
                .build();
    }

    private DataSourceFreshnessDTO.SummaryDTO summary(List<DataSourceFreshnessDTO.ItemDTO> items) {
        return DataSourceFreshnessDTO.SummaryDTO.builder()
                .total(items.size())
                .fresh(count(items, "fresh"))
                .lagging(count(items, "lagging"))
                .stale(count(items, "stale"))
                .empty(count(items, "empty"))
                .untracked(count(items, "untracked"))
                .error(count(items, "error"))
                .build();
    }

    private int count(List<DataSourceFreshnessDTO.ItemDTO> items, String status) {
        return (int) items.stream().filter(item -> status.equals(item.getStatus())).count();
    }
}
