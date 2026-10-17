package cn.cccs7.cynexus.gateway;

import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import cn.cccs7.cynexus.gateway.auth.OwnerAuthWebFilter;
import cn.cccs7.cynexus.gateway.auth.domain.OwnerSessionStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Adds a gateway-owned identity assertion; client-supplied identity headers are discarded. */
@Component
public class TrustedPrincipalGatewayFilter implements GlobalFilter, Ordered {

    private final String principal;
    private final String tenant;
    private final String secret;

    public TrustedPrincipalGatewayFilter(
            @Value("${cynexus.gateway.identity.principal:${CYNEXUS_GATEWAY_PRINCIPAL:workspace-user}}") String principal,
            @Value("${cynexus.gateway.identity.tenant:${CYNEXUS_GATEWAY_TENANT:default}}") String tenant,
            @Value("${cynexus.gateway.identity.secret:${CYNEXUS_TRUSTED_IDENTITY_SECRET:}}") String secret) {
        this.principal = principal;
        this.tenant = tenant;
        this.secret = secret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-Cynexus-Principal");
                    headers.remove("X-Cynexus-Tenant");
                    headers.remove("X-Cynexus-Identity-Signature");
                    if (!secret.isBlank() && exchange.getAttribute(OwnerAuthWebFilter.SESSION_ATTRIBUTE) instanceof OwnerSessionStore.Session) {
                        headers.set("X-Cynexus-Principal", principal);
                        headers.set("X-Cynexus-Tenant", tenant);
                        headers.set("X-Cynexus-Identity-Signature", TrustedPrincipalSigner.sign(principal, tenant, secret));
                    }
                }).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
