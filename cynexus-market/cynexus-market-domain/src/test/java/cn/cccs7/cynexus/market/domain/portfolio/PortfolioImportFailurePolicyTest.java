package cn.cccs7.cynexus.market.domain.portfolio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioImportFailurePolicyTest {
    @Test
    void failsImmediatelyForDisabledProviderGroup() {
        PortfolioImportFailureDecision decision = PortfolioImportFailurePolicy.decide(
                new PortfolioTradeVisionException("PORTFOLIO_AI_GROUP_DISABLED",
                        "AI provider rejected this request (HTTP 403): GROUP_DISABLED - API Key group is disabled.", false),
                0);

        assertFalse(decision.retry());
        assertTrue(decision.errorMessage().contains("GROUP_DISABLED"));
    }

    @Test
    void retriesTransientVisionFailureWithinBound() {
        PortfolioImportFailureDecision decision = PortfolioImportFailurePolicy.decide(
                new PortfolioTradeVisionException("PORTFOLIO_AI_SERVICE_UNAVAILABLE", "AI service unavailable", true), 2);

        assertTrue(decision.retry());
        assertTrue(decision.errorCode().equals("PORTFOLIO_PARSE_RETRY"));
    }

    @Test
    void failsTransientVisionFailureAfterRetryBound() {
        PortfolioImportFailureDecision decision = PortfolioImportFailurePolicy.decide(
                new PortfolioTradeVisionException("PORTFOLIO_AI_SERVICE_UNAVAILABLE", "AI service unavailable", true), 3);

        assertFalse(decision.retry());
        assertTrue(decision.errorCode().equals("PORTFOLIO_AI_SERVICE_UNAVAILABLE"));
    }
}
