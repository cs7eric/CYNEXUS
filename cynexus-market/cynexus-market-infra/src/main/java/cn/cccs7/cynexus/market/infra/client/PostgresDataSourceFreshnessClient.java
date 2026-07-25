package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.market.infra.common.constant.MarketInfraConstants;
import cn.cccs7.cynexus.market.domain.entity.DataSourceFreshnessBO;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostgresDataSourceFreshnessClient {

    private static final ZoneId STATUS_ZONE = ZoneId.of(CommonConstants.SHANGHAI_ZONE_ID);

    private final JdbcTemplate jdbcTemplate;

    public PostgresDataSourceFreshnessClient(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DataSourceFreshnessBO.ItemBO> snapshot() {
        try {
            String schema = jdbcTemplate.queryForObject("select current_schema()", String.class);
            String database = jdbcTemplate.queryForObject("select current_database()", String.class);
            List<TableRef> tables = jdbcTemplate.query("""
                            select table_schema, table_name
                            from information_schema.tables
                            where table_schema = current_schema()
                              and table_type = 'BASE TABLE'
                            order by table_name
                            """,
                    (rs, rowNum) -> new TableRef(rs.getString("table_schema"), rs.getString("table_name")));
            return tables.stream()
                    .map(table -> inspectTable(database, table))
                    .toList();
        } catch (Exception ex) {
            return List.of(errorItem(null, null, ex));
        }
    }

    private DataSourceFreshnessBO.ItemBO inspectTable(String database, TableRef table) {
        try {
            List<ColumnRef> columns = columns(table);
            Set<String> columnNames = new HashSet<>(columns.stream().map(ColumnRef::name).toList());
            String effectiveDateColumn = chooseEffectiveDateColumn(columns).orElse(null);
            String latestUpdatedColumn = columnNames.contains("updated_at") ? "updated_at" : null;
            if (effectiveDateColumn == null) {
                Long rowCount = rowCount(table, columnNames);
                return base(database, table)
                        .rowCount(rowCount)
                        .status(rowCount != null && rowCount == 0 ? "empty" : "untracked")
                        .build();
            }
            Map<String, Object> metrics = metrics(table, columnNames, effectiveDateColumn, latestUpdatedColumn);
            long rowCount = number(metrics.get("row_count"));
            String latestEffectiveDate = string(metrics.get("latest_effective_date"));
            String latestUpdatedAt = string(metrics.get("latest_updated_at"));
            Long lagDays = lagDays(latestEffectiveDate).orElse(null);
            return base(database, table)
                    .effectiveDateColumn(effectiveDateColumn)
                    .latestEffectiveDate(rowCount == 0 ? null : latestEffectiveDate)
                    .latestUpdatedAt(latestUpdatedAt)
                    .rowCount(rowCount)
                    .lagDays(rowCount == 0 ? null : lagDays)
                    .status(status(rowCount, lagDays))
                    .build();
        } catch (Exception ex) {
            return errorItem(database, table, ex);
        }
    }

    private List<ColumnRef> columns(TableRef table) {
        return jdbcTemplate.query("""
                        select column_name, data_type
                        from information_schema.columns
                        where table_schema = ?
                          and table_name = ?
                        """,
                (rs, rowNum) -> new ColumnRef(rs.getString("column_name"), rs.getString("data_type")),
                table.schemaName(), table.tableName());
    }

    private Optional<String> chooseEffectiveDateColumn(List<ColumnRef> columns) {
        return columns.stream()
                .filter(column -> isDateLikeType(column.dataType()))
                .min(Comparator.comparingInt(column -> priority(column.name())))
                .map(ColumnRef::name);
    }

    private boolean isDateLikeType(String dataType) {
        String value = dataType == null ? "" : dataType.toLowerCase(Locale.ROOT);
        return value.contains("date") || value.contains("timestamp");
    }

    private int priority(String columnName) {
        int index = MarketInfraConstants.DATE_COLUMN_PRIORITY.indexOf(columnName);
        if (index >= 0) {
            return index;
        }
        if (columnName != null && columnName.endsWith("_date")) {
            return MarketInfraConstants.DATE_COLUMN_PRIORITY.size();
        }
        if (columnName != null && columnName.endsWith("_at")) {
            return MarketInfraConstants.DATE_COLUMN_PRIORITY.size() + 10;
        }
        return MarketInfraConstants.DATE_COLUMN_PRIORITY.size() + 20;
    }

    private Long rowCount(TableRef table, Set<String> columnNames) {
        String where = columnNames.contains("deleted_at") ? " where " + quote("deleted_at") + " is null" : "";
        return jdbcTemplate.queryForObject("select count(*) from " + qualified(table) + where, Long.class);
    }

    private Map<String, Object> metrics(TableRef table,
                                        Set<String> columnNames,
                                        String effectiveDateColumn,
                                        String latestUpdatedColumn) {
        String latestUpdatedExpr = latestUpdatedColumn == null
                ? "null::text"
                : "max(" + quote(latestUpdatedColumn) + ")::text";
        String where = columnNames.contains("deleted_at") ? " where " + quote("deleted_at") + " is null" : "";
        String sql = "select count(*) as row_count, max(" + quote(effectiveDateColumn) + ")::text as latest_effective_date, "
                + latestUpdatedExpr + " as latest_updated_at from " + qualified(table) + where;
        return jdbcTemplate.queryForMap(sql);
    }

    private DataSourceFreshnessBO.ItemBO.ItemBOBuilder base(String database, TableRef table) {
        return DataSourceFreshnessBO.ItemBO.builder()
                .sourceType("postgres")
                .databaseName(database)
                .schemaName(table.schemaName())
                .tableName(table.tableName());
    }

    private DataSourceFreshnessBO.ItemBO errorItem(String database, TableRef table, Exception ex) {
        return DataSourceFreshnessBO.ItemBO.builder()
                .sourceType("postgres")
                .databaseName(database)
                .schemaName(table == null ? null : table.schemaName())
                .tableName(table == null ? "(connection)" : table.tableName())
                .status("error")
                .error(ex.getMessage())
                .build();
    }

    private String qualified(TableRef table) {
        return quote(table.schemaName()) + "." + quote(table.tableName());
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0 : Long.parseLong(value.toString());
    }

    private String string(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toString();
        }
        return value.toString();
    }

    private Optional<Long> lagDays(String value) {
        if (value == null || value.length() < 10) {
            return Optional.empty();
        }
        try {
            LocalDate date = LocalDate.parse(value.substring(0, 10));
            return Optional.of(ChronoUnit.DAYS.between(date, LocalDate.now(STATUS_ZONE)));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String status(long rowCount, Long lagDays) {
        if (rowCount == 0) {
            return "empty";
        }
        if (lagDays == null) {
            return "untracked";
        }
        if (lagDays <= 3) {
            return "fresh";
        }
        if (lagDays <= 7) {
            return "lagging";
        }
        return "stale";
    }

    private record TableRef(String schemaName, String tableName) {
    }

    private record ColumnRef(String name, String dataType) {
    }
}
