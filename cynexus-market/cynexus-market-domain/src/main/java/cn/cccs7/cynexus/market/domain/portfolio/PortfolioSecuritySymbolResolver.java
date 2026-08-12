package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.Optional;

/**
 * Resolves an extracted security name only when the market master data has one
 * exact active match for the candidate instrument type.
 */
public interface PortfolioSecuritySymbolResolver {

    Optional<String> resolveExact(String securityName, PortfolioInstrumentType instrumentType);
}
