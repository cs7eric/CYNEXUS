package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.DataSourceFreshnessBO;
import cn.cccs7.cynexus.market.domain.repository.DataSourceFreshnessRepository;
import cn.cccs7.cynexus.market.infra.client.ClickHouseDataSourceFreshnessClient;
import cn.cccs7.cynexus.market.infra.client.PostgresDataSourceFreshnessClient;
import cn.cccs7.cynexus.market.infra.client.PostgresDataSourceTableMetadataClient;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DataSourceFreshnessRepositoryImpl implements DataSourceFreshnessRepository {

    private final PostgresDataSourceFreshnessClient postgresClient;
    private final ClickHouseDataSourceFreshnessClient clickHouseClient;
    private final PostgresDataSourceTableMetadataClient tableMetadataClient;

    public DataSourceFreshnessRepositoryImpl(PostgresDataSourceFreshnessClient postgresClient,
                                             ClickHouseDataSourceFreshnessClient clickHouseClient,
                                             PostgresDataSourceTableMetadataClient tableMetadataClient) {
        this.postgresClient = postgresClient;
        this.clickHouseClient = clickHouseClient;
        this.tableMetadataClient = tableMetadataClient;
    }

    @Override
    public DataSourceFreshnessBO snapshot() {
        List<DataSourceFreshnessBO.ItemBO> items = new ArrayList<>();
        items.addAll(postgresClient.snapshot());
        items.addAll(clickHouseClient.snapshot());
        tableMetadataClient.enrich(items);
        items.sort(Comparator
                .comparing(DataSourceFreshnessBO.ItemBO::getSourceType, Comparator.nullsLast(String::compareTo))
                .thenComparing(DataSourceFreshnessBO.ItemBO::getSchemaName, Comparator.nullsLast(String::compareTo))
                .thenComparing(DataSourceFreshnessBO.ItemBO::getTableName, Comparator.nullsLast(String::compareTo)));
        return DataSourceFreshnessBO.builder()
                .generatedAt(OffsetDateTime.now())
                .items(items)
                .build();
    }
}
