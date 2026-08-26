package cn.cccs7.cynexus.market.infra.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Latest THS industry constituent snapshot, aligned to the requested close date. */
@Component
public class PostgresStockScreenerIndustryClient {

    private final JdbcTemplate jdbcTemplate;

    public PostgresStockScreenerIndustryClient(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, IndustryMetrics> findOnTradeDate(LocalDate tradeDate) {
        if (tradeDate == null) return Map.of();
        return jdbcTemplate.query(
                """
                select c.stock_code, c.industry_code,
                       coalesce(nullif(trim(m.ths_name), ''), c.industry_code) as industry_name,
                       m.tdx_code6, m.tdx_name,
                       c.turnover_pct, c.volume_ratio, c.pe
                   from cynexus_appl_market.ths_industry_constituent_snapshot c
                  left join lateral (
                      select ths_name, tdx_code6, tdx_name from cynexus_appl_market.tdx_ths_industry_mapping
                       where deleted_at is null and ths_code = c.industry_code
                       order by is_primary desc, priority asc nulls last, confidence desc nulls last limit 1
                  ) m on true
                  where c.deleted_at is null and c.snapshot_date = ?
                """,
                resultSet -> {
                    Map<String, IndustryMetrics> rows = new java.util.HashMap<>();
                    while (resultSet.next()) {
                        rows.put(resultSet.getString("stock_code"), new IndustryMetrics(
                                resultSet.getString("industry_code"), resultSet.getString("industry_name"),
                                resultSet.getString("tdx_code6"), resultSet.getString("tdx_name"),
                                resultSet.getBigDecimal("turnover_pct"), resultSet.getBigDecimal("volume_ratio"),
                                resultSet.getBigDecimal("pe")
                        ));
                    }
                    return rows;
                }, tradeDate);
    }

    public List<IndustryOption> listOptionsOnOrBefore(LocalDate tradeDate) {
        if (tradeDate == null) return List.of();
        return jdbcTemplate.query(
                """
                with latest as (
                    select max(snapshot_date) as snapshot_date
                      from cynexus_appl_market.ths_industry_constituent_snapshot
                     where deleted_at is null and snapshot_date <= ?
                )
                select c.industry_code,
                       coalesce(nullif(trim(m.ths_name), ''), c.industry_code) as industry_name,
                       count(distinct c.stock_code) as stock_count,
                       max(c.snapshot_date) as snapshot_date
                  from cynexus_appl_market.ths_industry_constituent_snapshot c
                  join latest l on l.snapshot_date = c.snapshot_date
                  left join lateral (
                      select ths_name from cynexus_appl_market.tdx_ths_industry_mapping
                       where deleted_at is null and ths_code = c.industry_code
                       order by is_primary desc, priority asc nulls last, confidence desc nulls last limit 1
                  ) m on true
                 where c.deleted_at is null
                 group by c.industry_code, coalesce(nullif(trim(m.ths_name), ''), c.industry_code)
                 order by industry_name, c.industry_code
                """,
                (resultSet, rowNum) -> new IndustryOption(resultSet.getString("industry_code"),
                        resultSet.getString("industry_name"), resultSet.getInt("stock_count"),
                        resultSet.getObject("snapshot_date", LocalDate.class)), tradeDate);
    }

    public List<IndustryOption> listTdxGroupOptionsOnOrBefore(LocalDate tradeDate) {
        if (tradeDate == null) return List.of();
        return jdbcTemplate.query(
                """
                with latest as (
                    select max(snapshot_date) as snapshot_date
                      from cynexus_appl_market.ths_industry_constituent_snapshot
                     where deleted_at is null and snapshot_date <= ?
                )
                select m.tdx_code6 as industry_code, m.tdx_name as industry_name,
                       count(distinct c.stock_code) as stock_count, max(c.snapshot_date) as snapshot_date
                  from cynexus_appl_market.ths_industry_constituent_snapshot c
                  join latest l on l.snapshot_date = c.snapshot_date
                  join lateral (
                      select tdx_code6, tdx_name from cynexus_appl_market.tdx_ths_industry_mapping
                       where deleted_at is null and ths_code = c.industry_code and tdx_code6 is not null
                       order by is_primary desc, priority asc nulls last, confidence desc nulls last limit 1
                  ) m on true
                 where c.deleted_at is null
                 group by m.tdx_code6, m.tdx_name
                 order by industry_name, industry_code
                """,
                (resultSet, rowNum) -> new IndustryOption(resultSet.getString("industry_code"),
                        resultSet.getString("industry_name"), resultSet.getInt("stock_count"),
                        resultSet.getObject("snapshot_date", LocalDate.class)), tradeDate);
    }

    public record IndustryMetrics(String code, String name, String tdxCode, String tdxName,
                                  BigDecimal turnoverPct, BigDecimal volumeRatio, BigDecimal pe) {}
    public record IndustryOption(String code, String name, int stockCount, LocalDate snapshotDate) {}
}
