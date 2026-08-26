package cn.cccs7.cynexus.market.infra.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Read-only TDX concept and style memberships used by the screener. */
@Component
public class PostgresStockScreenerSectorClient {

    private final JdbcTemplate jdbcTemplate;

    public PostgresStockScreenerSectorClient(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Memberships> findMemberships() {
        return jdbcTemplate.query(
                """
                select ss.code, s.category_label, s.sector_id, s.name
                  from cynexus_appl_market.sec_stock_sector ss
                  join cynexus_appl_market.sec_sector s on s.sector_id = ss.sector_id
                 where s.category_label in ('concepts', 'styles')
                 order by ss.code, s.category_label, s.name, s.sector_id
                """,
                resultSet -> {
                    Map<String, MutableMemberships> mutable = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        MutableMemberships current = mutable.computeIfAbsent(resultSet.getString("code"), ignored -> new MutableMemberships());
                        SectorMembership membership = new SectorMembership(resultSet.getString("sector_id"), resultSet.getString("name"));
                        if ("concepts".equals(resultSet.getString("category_label"))) {
                            current.concepts.add(membership);
                        } else {
                            current.styles.add(membership);
                        }
                    }
                    Map<String, Memberships> result = new LinkedHashMap<>();
                    mutable.forEach((code, value) -> result.put(code, new Memberships(List.copyOf(value.concepts), List.copyOf(value.styles))));
                    return result;
                });
    }

    public List<SectorOption> listOptions(String categoryLabel) {
        return jdbcTemplate.query(
                """
                select s.sector_id, s.name, count(distinct ss.code) as stock_count
                  from cynexus_appl_market.sec_sector s
                  join cynexus_appl_market.sec_stock_sector ss on ss.sector_id = s.sector_id
                 where s.category_label = ?
                 group by s.sector_id, s.name
                 order by s.name, s.sector_id
                """,
                (resultSet, rowNum) -> new SectorOption(resultSet.getString("sector_id"), resultSet.getString("name"),
                        resultSet.getInt("stock_count")), categoryLabel);
    }

    public record Memberships(List<SectorMembership> concepts, List<SectorMembership> styles) {}
    public record SectorMembership(String sectorId, String name) {}
    public record SectorOption(String sectorId, String name, int stockCount) {}

    private static final class MutableMemberships {
        private final List<SectorMembership> concepts = new ArrayList<>();
        private final List<SectorMembership> styles = new ArrayList<>();
    }
}
