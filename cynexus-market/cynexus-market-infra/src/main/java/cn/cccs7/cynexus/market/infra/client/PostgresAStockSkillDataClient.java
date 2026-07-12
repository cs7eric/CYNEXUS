package cn.cccs7.cynexus.market.infra.client;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostgresAStockSkillDataClient {

    private final JdbcTemplate jdbcTemplate;

    public PostgresAStockSkillDataClient(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TableStats stats(String schemaName, String tableName, String dateColumn) {
        String latestExpr = dateColumn == null || dateColumn.isBlank() ? "null::text" : "max(" + quote(dateColumn) + ")::text";
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select count(*) as row_count, " + latestExpr + " as latest_at from " + qualified(schemaName, tableName)
        );
        return new TableStats(number(row.get("row_count")), string(row.get("latest_at")));
    }

    public List<Map<String, Object>> rows(String schemaName,
                                          String tableName,
                                          List<String> columns,
                                          String sourceColumn,
                                          String codeColumn,
                                          String sortClause,
                                          int limit,
                                          String code,
                                          String source) {
        List<Object> args = new ArrayList<>();
        List<String> where = new ArrayList<>();
        if (sourceColumn != null && source != null && !source.isBlank()) {
            where.add(quote(sourceColumn) + " = ?");
            args.add(source.trim());
        }
        if (codeColumn != null && code != null && !code.isBlank()) {
            where.add(quote(codeColumn) + " = ?");
            args.add(code.trim());
        }
        String sql = "select " + selectColumns(columns)
                + " from " + qualified(schemaName, tableName)
                + (where.isEmpty() ? "" : " where " + String.join(" and ", where))
                + " order by " + sortClause
                + " limit ?";
        args.add(Math.max(1, Math.min(limit, 500)));
        return jdbcTemplate.queryForList(sql, args.toArray()).stream()
                .map(this::normalizeRow)
                .toList();
    }

    public List<FundFlowDailyRow> listFundFlowDaily(String code, int limit) {
        return jdbcTemplate.query(
                """
                select distinct on (trade_date)
                       trade_date, main_net, super_net, large_net, mid_net, small_net, source
                  from cynexus_appl_market.a_stock_fund_flow_daily
                 where code = ?
                 order by trade_date desc,
                          case when source = 'eastmoney.push2his' then 0 else 1 end,
                          ingested_at desc
                 limit ?
                """,
                (rs, rowNum) -> new FundFlowDailyRow(
                        rs.getObject("trade_date", LocalDate.class),
                        rs.getBigDecimal("main_net"),
                        rs.getBigDecimal("super_net"),
                        rs.getBigDecimal("large_net"),
                        rs.getBigDecimal("mid_net"),
                        rs.getBigDecimal("small_net"),
                        rs.getString("source")
                ),
                code,
                Math.max(1, Math.min(limit, 120))
        );
    }

    private String selectColumns(List<String> columns) {
        return columns.stream().map(this::quote).reduce((left, right) -> left + ", " + right).orElse("*");
    }

    private Map<String, Object> normalizeRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> normalized.put(key, normalizeValue(value)));
        return normalized;
    }

    private Object normalizeValue(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toString();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if ("org.postgresql.util.PGobject".equals(value.getClass().getName())) {
            return value.toString();
        }
        return value.toString();
    }

    private String qualified(String schemaName, String tableName) {
        return quote(schemaName) + "." + quote(tableName);
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(value.toString());
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    public record TableStats(Long rowCount, String latestAt) {
    }

    public record FundFlowDailyRow(LocalDate tradeDate,
                                   BigDecimal mainNet,
                                   BigDecimal superNet,
                                   BigDecimal largeNet,
                                   BigDecimal midNet,
                                   BigDecimal smallNet,
                                   String source) {
    }
}
