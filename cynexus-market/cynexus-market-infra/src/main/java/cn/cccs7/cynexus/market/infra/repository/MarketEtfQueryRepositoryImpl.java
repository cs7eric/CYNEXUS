package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketEtfDataBO;
import cn.cccs7.cynexus.market.domain.repository.MarketEtfQueryRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MarketEtfQueryRepositoryImpl implements MarketEtfQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public MarketEtfQueryRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MarketEtfDataBO load(int historyDays, int rowLimit) {
        List<Map<String, Object>> spotRows = jdbcTemplate.queryForList(
                """
                WITH latest_date AS (
                    SELECT max(trade_date) AS trade_date
                      FROM market_etf_spot_snapshot
                     WHERE deleted_at IS NULL
                ), latest_codes AS (
                    SELECT DISTINCT ON (code) *
                      FROM market_etf_spot_snapshot s
                      JOIN latest_date d ON d.trade_date = s.trade_date
                     WHERE s.deleted_at IS NULL
                       AND s.category IN ('broad', 'sector')
                     ORDER BY code, snapshot_minute DESC, captured_at DESC
                )
                SELECT latest_codes.* FROM latest_codes
                ORDER BY amount DESC NULLS LAST, code
                """);
        LocalDate tradeDate = spotRows.stream().map(row -> localDate(row.get("trade_date"))).filter(Objects::nonNull).findFirst().orElse(null);
        List<Map<String, Object>> dailyRows = jdbcTemplate.queryForList(
                """
                SELECT *
                  FROM market_etf_daily
                 WHERE deleted_at IS NULL
                   AND category IN ('broad', 'sector')
                   AND trade_date >= COALESCE((SELECT max(trade_date) FROM market_etf_daily WHERE deleted_at IS NULL), CURRENT_DATE) - ((? - 1) * INTERVAL '1 day')
                 ORDER BY trade_date ASC, code
                """, Math.max(1, historyDays));
        Map<String, Object> breadth = jdbcTemplate.queryForList(
                """
                SELECT trade_date, stock_count, rising_count, falling_count, flat_count, limit_up_count, limit_down_count, source
                  FROM market_overview_snapshots
                 WHERE deleted_at IS NULL
                 ORDER BY trade_date DESC
                LIMIT 1
                """).stream().findFirst().orElse(Map.of());
        Map<String, Object> mappingStats = jdbcTemplate.queryForList(
                """
                WITH catalog AS (
                    SELECT code
                      FROM market_etf_catalog
                     WHERE deleted_at IS NULL
                       AND is_active = TRUE
                ), mapping AS (
                    SELECT m.*
                      FROM market_etf_mapping m
                      JOIN catalog c ON c.code = m.etf_code
                     WHERE m.deleted_at IS NULL
                )
                SELECT
                    (SELECT count(*) FROM catalog) AS catalog_etf_count,
                    (SELECT count(DISTINCT etf_code) FROM mapping) AS mapped_etf_count,
                    (SELECT count(*) FROM mapping) AS relationship_count,
                    (SELECT source FROM mapping GROUP BY source ORDER BY count(*) DESC, source LIMIT 1) AS source
                """).stream().findFirst().orElse(Map.of());
        List<Map<String, Object>> mappingDimensions = jdbcTemplate.queryForList(
                """
                SELECT m.mapping_group,
                       count(DISTINCT m.etf_code) AS etf_count,
                       count(*) AS relationship_count
                  FROM market_etf_mapping m
                  JOIN market_etf_catalog c ON c.code = m.etf_code
                 WHERE m.deleted_at IS NULL
                   AND c.deleted_at IS NULL
                   AND c.is_active = TRUE
                 GROUP BY m.mapping_group
                 ORDER BY CASE m.mapping_group
                              WHEN 'universe' THEN 1
                              WHEN 'asset_class' THEN 2
                              WHEN 'region' THEN 3
                              WHEN 'index_scope' THEN 4
                              WHEN 'style' THEN 5
                              WHEN 'theme' THEN 6
                              ELSE 99
                          END
                """);
        List<Map<String, Object>> mappingTags = jdbcTemplate.queryForList(
                """
                WITH grouped AS (
                    SELECT m.mapping_group, m.mapping_code, m.mapping_name,
                           count(DISTINCT m.etf_code) AS etf_count,
                           count(*) AS relationship_count
                      FROM market_etf_mapping m
                      JOIN market_etf_catalog c ON c.code = m.etf_code
                     WHERE m.deleted_at IS NULL
                       AND c.deleted_at IS NULL
                       AND c.is_active = TRUE
                     GROUP BY m.mapping_group, m.mapping_code, m.mapping_name
                ), ranked AS (
                    SELECT grouped.*, row_number() OVER (
                        PARTITION BY mapping_group ORDER BY etf_count DESC, mapping_name
                    ) AS rank_in_group
                      FROM grouped
                )
                SELECT mapping_group, mapping_code, mapping_name, etf_count, relationship_count
                  FROM ranked
                 WHERE rank_in_group <= 16
                 ORDER BY mapping_group, etf_count DESC, mapping_name
                """);
        return MarketEtfDataBO.builder()
                .tradeDate(tradeDate)
                .fetchedAt(spotRows.stream().map(row -> offsetDateTime(row.get("captured_at"))).filter(Objects::nonNull).findFirst().orElse(null))
                .source(spotRows.stream().map(row -> text(row.get("source"))).filter(Objects::nonNull).findFirst().orElse("postgres.market_etf"))
                .spots(spotRows.stream().map(this::spot).toList())
                .dailies(dailyRows.stream().map(this::daily).toList())
                .breadth(breadth(breadth))
                .mapping(mapping(mappingStats, mappingDimensions, mappingTags))
                .build();
    }

    private MarketEtfDataBO.MappingSummary mapping(Map<String, Object> stats,
                                                    List<Map<String, Object>> dimensions,
                                                    List<Map<String, Object>> tags) {
        if (stats.isEmpty()) return null;
        Integer catalogCountValue = integer(stats.get("catalog_etf_count"));
        Integer mappedCountValue = integer(stats.get("mapped_etf_count"));
        int catalogCount = catalogCountValue == null ? 0 : catalogCountValue;
        int mappedCount = mappedCountValue == null ? 0 : mappedCountValue;
        return MarketEtfDataBO.MappingSummary.builder()
                .catalogEtfCount(catalogCount)
                .mappedEtfCount(mappedCount)
                .unmappedEtfCount(Math.max(0, catalogCount - mappedCount))
                .relationshipCount(integer(stats.get("relationship_count")))
                .source(text(stats.get("source")))
                .dimensions(dimensions.stream().map(row -> MarketEtfDataBO.MappingDimension.builder()
                        .group(text(row.get("mapping_group")))
                        .etfCount(integer(row.get("etf_count")))
                        .relationshipCount(integer(row.get("relationship_count")))
                        .build()).toList())
                .tags(tags.stream().map(row -> MarketEtfDataBO.MappingTag.builder()
                        .group(text(row.get("mapping_group")))
                        .code(text(row.get("mapping_code")))
                        .name(text(row.get("mapping_name")))
                        .etfCount(integer(row.get("etf_count")))
                        .relationshipCount(integer(row.get("relationship_count")))
                        .build()).toList())
                .build();
    }

    private MarketEtfDataBO.Spot spot(Map<String, Object> row) {
        return MarketEtfDataBO.Spot.builder().code(text(row.get("code"))).exchange(text(row.get("exchange"))).name(text(row.get("name")))
                .category(text(row.get("category"))).theme(text(row.get("theme"))).classificationSource(text(row.get("classification_source"))).price(decimal(row.get("price"))).prevClose(decimal(row.get("prev_close")))
                .changePct(decimal(row.get("change_pct"))).volume(decimal(row.get("volume"))).amount(decimal(row.get("amount"))).iopv(decimal(row.get("iopv")))
                .premiumPct(decimal(row.get("premium_pct"))).turnoverPct(decimal(row.get("turnover_pct"))).insideVolume(decimal(row.get("inside_volume")))
                .outsideVolume(decimal(row.get("outside_volume"))).latestShares(decimal(row.get("latest_shares")))
                .superLargeNetInflow(decimal(row.get("super_large_net_inflow"))).superLargeNetRatio(decimal(row.get("super_large_net_ratio")))
                .mainNetInflow(decimal(row.get("main_net_inflow"))).mainNetRatio(decimal(row.get("main_net_ratio")))
                .largeNetInflow(decimal(row.get("large_net_inflow"))).largeNetRatio(decimal(row.get("large_net_ratio")))
                .mediumNetInflow(decimal(row.get("medium_net_inflow"))).mediumNetRatio(decimal(row.get("medium_net_ratio")))
                .smallNetInflow(decimal(row.get("small_net_inflow"))).smallNetRatio(decimal(row.get("small_net_ratio")))
                .capturedAt(offsetDateTime(row.get("captured_at"))).build();
    }

    private MarketEtfDataBO.Daily daily(Map<String, Object> row) {
        return MarketEtfDataBO.Daily.builder().tradeDate(localDate(row.get("trade_date"))).code(text(row.get("code"))).exchange(text(row.get("exchange")))
                .name(text(row.get("name"))).category(text(row.get("category"))).theme(text(row.get("theme"))).closePrice(decimal(row.get("close_price")))
                .iopv(decimal(row.get("iopv"))).premiumPct(decimal(row.get("premium_pct"))).volume(decimal(row.get("volume"))).amount(decimal(row.get("amount")))
                .totalShares(decimal(row.get("total_shares"))).previousShares(decimal(row.get("previous_shares"))).shareDelta(decimal(row.get("share_delta")))
                .netSubscriptionProxy(decimal(row.get("net_subscription_proxy"))).cumulativeShareDelta(decimal(row.get("cumulative_share_delta")))
                .cumulativeNetSubscriptionProxy(decimal(row.get("cumulative_net_subscription_proxy"))).turnoverPct(decimal(row.get("turnover_pct")))
                .insideVolume(decimal(row.get("inside_volume"))).outsideVolume(decimal(row.get("outside_volume")))
                .superLargeNetInflow(decimal(row.get("super_large_net_inflow"))).superLargeNetRatio(decimal(row.get("super_large_net_ratio")))
                .mainNetInflow(decimal(row.get("main_net_inflow"))).mainNetRatio(decimal(row.get("main_net_ratio")))
                .largeNetInflow(decimal(row.get("large_net_inflow"))).largeNetRatio(decimal(row.get("large_net_ratio")))
                .mediumNetInflow(decimal(row.get("medium_net_inflow"))).mediumNetRatio(decimal(row.get("medium_net_ratio")))
                .smallNetInflow(decimal(row.get("small_net_inflow"))).smallNetRatio(decimal(row.get("small_net_ratio"))).build();
    }

    private MarketEtfDataBO.Breadth breadth(Map<String, Object> row) {
        if (row.isEmpty()) return null;
        return MarketEtfDataBO.Breadth.builder().tradeDate(localDate(row.get("trade_date"))).stockCount(integer(row.get("stock_count")))
                .risingCount(integer(row.get("rising_count"))).fallingCount(integer(row.get("falling_count"))).flatCount(integer(row.get("flat_count")))
                .limitUpCount(integer(row.get("limit_up_count"))).limitDownCount(integer(row.get("limit_down_count"))).source(text(row.get("source"))).build();
    }

    private static String text(Object value) { return value == null ? null : String.valueOf(value); }
    private static Integer integer(Object value) { return value instanceof Number number ? number.intValue() : null; }
    private static BigDecimal decimal(Object value) { return value instanceof BigDecimal decimal ? decimal : value instanceof Number number ? BigDecimal.valueOf(number.doubleValue()) : null; }
    private static LocalDate localDate(Object value) { return value instanceof LocalDate date ? date : value instanceof java.sql.Date date ? date.toLocalDate() : null; }
    private static OffsetDateTime offsetDateTime(Object value) {
        if (value instanceof OffsetDateTime dateTime) return dateTime;
        if (value instanceof Timestamp timestamp) return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        return null;
    }
}
