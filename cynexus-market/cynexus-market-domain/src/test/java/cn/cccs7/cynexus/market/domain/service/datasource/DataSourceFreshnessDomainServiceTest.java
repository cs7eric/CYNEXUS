package cn.cccs7.cynexus.market.domain.service.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.cccs7.cynexus.market.domain.entity.DataSourceFreshnessBO;
import cn.cccs7.cynexus.market.domain.repository.DataSourceFreshnessRepository;
import cn.cccs7.cynexus.market.domain.service.datasource.impl.DataSourceFreshnessDomainServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;

class DataSourceFreshnessDomainServiceTest {

    @Test
    void computesFreshnessSummaryFromDomainItems() {
        DataSourceFreshnessRepository repository = () -> DataSourceFreshnessBO.builder()
                .items(List.of(item("fresh"), item("stale"), item("error"))).build();

        DataSourceFreshnessBO value = new DataSourceFreshnessDomainServiceImpl(repository).snapshot();

        assertEquals(3, value.getSummary().getTotal());
        assertEquals(1, value.getSummary().getFresh());
        assertEquals(1, value.getSummary().getStale());
        assertEquals(1, value.getSummary().getError());
    }

    private DataSourceFreshnessBO.ItemBO item(String status) {
        return DataSourceFreshnessBO.ItemBO.builder().status(status).build();
    }
}
