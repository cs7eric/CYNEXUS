package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioInstrumentType;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioSecuritySymbolResolver;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioSecuritySymbolResolverAdapter implements PortfolioSecuritySymbolResolver {

    private static final int UNIQUE_MATCH_LIMIT = 2;
    private final JdbcTemplate jdbcTemplate;

    public PortfolioSecuritySymbolResolverAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<String> resolveExact(String securityName, PortfolioInstrumentType instrumentType) {
        String normalizedName = securityName == null ? "" : securityName.trim();
        if (normalizedName.isBlank() || instrumentType == null) {
            return Optional.empty();
        }
        List<String> symbols = instrumentType == PortfolioInstrumentType.ETF
                ? findExactEtfSymbols(normalizedName)
                : findExactStockSymbols(normalizedName);
        return symbols.stream().distinct().limit(UNIQUE_MATCH_LIMIT).toList().size() == 1
                ? Optional.of(symbols.getFirst())
                : Optional.empty();
    }

    private List<String> findExactStockSymbols(String securityName) {
        return jdbcTemplate.queryForList(
                """
                SELECT code
                  FROM sec_stock_basic
                 WHERE is_active IS TRUE
                   AND btrim(name) = ?
                 ORDER BY code
                 LIMIT ?
                """,
                String.class, securityName, UNIQUE_MATCH_LIMIT);
    }

    private List<String> findExactEtfSymbols(String securityName) {
        return jdbcTemplate.queryForList(
                """
                SELECT code
                  FROM market_etf_catalog
                 WHERE is_active IS TRUE
                   AND deleted_at IS NULL
                   AND btrim(name) = ?
                 ORDER BY code
                 LIMIT ?
                """,
                String.class, securityName, UNIQUE_MATCH_LIMIT);
    }
}
