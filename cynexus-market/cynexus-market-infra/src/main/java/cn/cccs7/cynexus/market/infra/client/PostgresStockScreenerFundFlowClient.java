package cn.cccs7.cynexus.market.infra.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Bounded all-market main-fund-flow snapshot used to enrich stock screens. */
@Component
public class PostgresStockScreenerFundFlowClient {

    private final JdbcTemplate jdbcTemplate;

    public PostgresStockScreenerFundFlowClient(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, FundFlowSummary> findOnTradeDate(LocalDate tradeDate) {
        if (tradeDate == null) {
            return Map.of();
        }
        return jdbcTemplate.query(
                """
                with deduplicated as (
                    select code, trade_date, main_net,
                           row_number() over (
                               partition by code, trade_date
                               order by case when source = 'easy_tdx.tdx' then 0
                                             when source = 'eastmoney.push2his' then 1
                                             else 2 end, ingested_at desc
                           ) as source_rank
                      from cynexus_appl_market.a_stock_fund_flow_daily
                     where trade_date <= ?
                ), ranked as (
                    select code, trade_date, main_net,
                           row_number() over (partition by code order by trade_date desc) as session_rank
                      from deduplicated
                     where source_rank = 1
                )
                select code,
                        max(main_net) filter (where session_rank = 1) as main_net,
                        sum(main_net) filter (where session_rank <= 5) as main_net_5d
                  from ranked
                 where session_rank <= 5
                  group by code
                having max(trade_date) filter (where session_rank = 1) = ?
                """,
                resultSet -> {
                    Map<String, FundFlowSummary> rows = new java.util.HashMap<>();
                    while (resultSet.next()) {
                        rows.put(resultSet.getString("code"), new FundFlowSummary(
                                resultSet.getBigDecimal("main_net"), resultSet.getBigDecimal("main_net_5d")
                        ));
                    }
                    return rows;
                }, tradeDate, tradeDate
        );
    }

    public record FundFlowSummary(BigDecimal mainNet, BigDecimal mainNet5d) {
    }
}
