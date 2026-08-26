package cn.cccs7.cynexus.market.application.service.stockselection;

import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunBO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StockSelectionIdentityResolver {

    private final String secret;
    private final boolean allowSingleUserFallback;

    public StockSelectionIdentityResolver(
            @Value("${cynexus.market.screener.trusted-identity-secret:${CYNEXUS_TRUSTED_IDENTITY_SECRET:}}") String secret,
            @Value("${cynexus.market.screener.allow-single-user-fallback:${CYNEXUS_MARKET_SCREENER_ALLOW_SINGLE_USER_FALLBACK:true}}") boolean allowSingleUserFallback) {
        this.secret = secret;
        this.allowSingleUserFallback = allowSingleUserFallback;
    }

    public StockSelectionStrategyRunBO.Scope resolve(String principal, String tenant, String signature) {
        if (TrustedPrincipalSigner.verify(principal, tenant, signature, secret)) {
            return new StockSelectionStrategyRunBO.Scope(tenant.trim(), principal.trim());
        }
        if (allowSingleUserFallback && blank(principal) && blank(tenant) && blank(signature)) {
            return new StockSelectionStrategyRunBO.Scope("default", "workspace-user");
        }
        throw new IllegalStateException("A trusted stock selection identity is required");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
