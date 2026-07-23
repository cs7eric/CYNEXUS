package cn.cccs7.cynexus.market.domain.service.datasource.impl;

import cn.cccs7.cynexus.market.domain.entity.DataSourceFreshnessBO;
import cn.cccs7.cynexus.market.domain.repository.DataSourceFreshnessRepository;
import cn.cccs7.cynexus.market.domain.service.datasource.DataSourceFreshnessDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DataSourceFreshnessDomainServiceImpl implements DataSourceFreshnessDomainService {

    private final DataSourceFreshnessRepository repository;

    public DataSourceFreshnessDomainServiceImpl(DataSourceFreshnessRepository repository) {
        this.repository = repository;
    }

    @Override
    public DataSourceFreshnessBO snapshot() {
        DataSourceFreshnessBO value = repository.snapshot();
        List<DataSourceFreshnessBO.ItemBO> items = value == null || value.getItems() == null
                ? List.of() : value.getItems();
        return DataSourceFreshnessBO.builder()
                .generatedAt(value == null ? null : value.getGeneratedAt())
                .items(items)
                .summary(summary(items))
                .build();
    }

    private DataSourceFreshnessBO.SummaryBO summary(List<DataSourceFreshnessBO.ItemBO> items) {
        return DataSourceFreshnessBO.SummaryBO.builder()
                .total(items.size()).fresh(count(items, "fresh")).lagging(count(items, "lagging"))
                .stale(count(items, "stale")).empty(count(items, "empty"))
                .untracked(count(items, "untracked")).error(count(items, "error")).build();
    }

    private int count(List<DataSourceFreshnessBO.ItemBO> items, String status) {
        return (int) items.stream().filter(item -> status.equals(item.getStatus())).count();
    }
}
