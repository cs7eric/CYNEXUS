package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.market.domain.entity.DataSourceFreshnessBO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class PostgresDataSourceTableMetadataClient {

    private static final Logger log = LoggerFactory.getLogger(PostgresDataSourceTableMetadataClient.class);
    private static final String TABLE_NAME = "mkt_data_source_table_metadata";
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean metadataInitialized;

    public PostgresDataSourceTableMetadataClient(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void enrich(List<DataSourceFreshnessBO.ItemBO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        try {
            ensureMetadataReady();
            Map<String, TableMetadata> metadataByKey = loadMetadata().stream()
                    .collect(Collectors.toMap(
                            metadata -> key(metadata.sourceType(), metadata.schemaName(), metadata.tableName()),
                            Function.identity(),
                            (first, ignored) -> first
                    ));
            items.forEach(item -> enrich(item, metadataByKey));
        } catch (Exception ex) {
            log.warn("Data source table metadata enrichment failed", ex);
        }
    }

    private void ensureMetadataReady() throws Exception {
        if (metadataInitialized) {
            return;
        }
        synchronized (this) {
            if (metadataInitialized) {
                return;
            }
            ensureTable();
            seedDefaultMetadata();
            metadataInitialized = true;
        }
    }

    private void ensureTable() {
        jdbcTemplate.execute("""
                create table if not exists mkt_data_source_table_metadata (
                    source_type varchar(32) not null,
                    schema_name varchar(128) not null default '',
                    table_name varchar(128) not null,
                    business_logic text,
                    used_by jsonb not null default '[]'::jsonb,
                    update_job varchar(256),
                    data_source varchar(256),
                    enabled boolean not null default true,
                    created_at timestamptz not null default now(),
                    updated_at timestamptz not null default now(),
                    primary key (source_type, schema_name, table_name)
                )
                """);
        jdbcTemplate.execute("""
                create index if not exists idx_mkt_data_source_table_metadata_enabled
                    on mkt_data_source_table_metadata (enabled, source_type, table_name)
                """);
        jdbcTemplate.execute("""
                comment on table mkt_data_source_table_metadata is 'Data Source 页面使用的表级业务元数据，记录业务逻辑、使用位置、更新 Job 和数据源'
                """);
    }

    private void seedDefaultMetadata() throws Exception {
        ClassPathResource resource = new ClassPathResource("data-source-table-metadata.sql");
        if (!resource.exists()) {
            return;
        }
        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        if (!sql.isBlank()) {
            jdbcTemplate.execute(sql);
        }
    }

    private List<TableMetadata> loadMetadata() {
        return jdbcTemplate.query("""
                        select source_type,
                               coalesce(schema_name, '') as schema_name,
                               table_name,
                               business_logic,
                               used_by::text as used_by,
                               update_job,
                               data_source
                          from mkt_data_source_table_metadata
                         where enabled = true
                        """,
                this::mapRow);
    }

    private TableMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TableMetadata(
                rs.getString("source_type"),
                rs.getString("schema_name"),
                rs.getString("table_name"),
                rs.getString("business_logic"),
                parseUsedBy(rs.getString("used_by")),
                rs.getString("update_job"),
                rs.getString("data_source")
        );
    }

    private List<String> parseUsedBy(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse {}.used_by JSON: {}", TABLE_NAME, value);
            return List.of();
        }
    }

    private void enrich(DataSourceFreshnessBO.ItemBO item, Map<String, TableMetadata> metadataByKey) {
        TableMetadata metadata = metadataByKey.get(key(item.getSourceType(), item.getSchemaName(), item.getTableName()));
        if (metadata == null) {
            metadata = metadataByKey.get(key(item.getSourceType(), "", item.getTableName()));
        }
        if (metadata == null) {
            return;
        }
        item.setBusinessLogic(metadata.businessLogic());
        item.setUsedBy(metadata.usedBy());
        item.setUpdateJob(metadata.updateJob());
        item.setDataSource(metadata.dataSource());
    }

    private String key(String sourceType, String schemaName, String tableName) {
        return normalize(sourceType) + ":" + normalize(schemaName) + ":" + normalize(tableName);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private record TableMetadata(
            String sourceType,
            String schemaName,
            String tableName,
            String businessLogic,
            List<String> usedBy,
            String updateJob,
            String dataSource
    ) {
    }
}
