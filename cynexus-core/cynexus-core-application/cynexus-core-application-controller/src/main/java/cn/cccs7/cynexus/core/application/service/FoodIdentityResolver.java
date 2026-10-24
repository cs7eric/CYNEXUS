package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Never accepts owner IDs from query/body, nor unsigned direct service requests. */
@Component
public class FoodIdentityResolver {
    private final String secret;
    public FoodIdentityResolver(@Value("${cynexus.core.food.trusted-identity-secret:${CYNEXUS_TRUSTED_IDENTITY_SECRET:}}") String secret) { this.secret=secret; }
    public String owner(HttpServletRequest request) {
        return resolve(request).owner();
    }
    public Identity resolve(HttpServletRequest request) {
        String principal=request.getHeader("X-Cynexus-Principal"),tenant=request.getHeader("X-Cynexus-Tenant");
        if (!TrustedPrincipalSigner.verify(principal,tenant,request.getHeader("X-Cynexus-Identity-Signature"),secret))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"A trusted owner session is required");
        principal=principal.trim(); tenant=tenant.trim();
        return new Identity(tenant.length()+":"+tenant+principal.length()+":"+principal, principal, tenant);
    }

    public record Identity(String owner, String principal, String tenant) { }
}
