package cn.cccs7.cynexus.market.domain.portfolio;

/**
 * A review-time view of an import candidate. A suggested symbol is not stored
 * in the original candidate payload until the user saves or confirms it.
 */
public record PortfolioCandidateReview(PortfolioCandidateRecord candidate, String suggestedSymbol) {
}
