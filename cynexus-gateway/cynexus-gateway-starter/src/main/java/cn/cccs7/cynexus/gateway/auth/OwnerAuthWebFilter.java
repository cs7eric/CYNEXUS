package cn.cccs7.cynexus.gateway.auth;

import cn.cccs7.cynexus.gateway.auth.domain.OwnerAuthService;
import cn.cccs7.cynexus.gateway.auth.domain.OwnerSessionStore;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public final class OwnerAuthWebFilter implements WebFilter, Ordered {
    public static final String COOKIE = "cynexus_owner_session";
    public static final String SESSION_ATTRIBUTE = "owner-session";
    private final OwnerAuthService auth;

    public OwnerAuthWebFilter(OwnerAuthService auth) { this.auth = auth; }

    @Override public int getOrder() { return -200; }

    @Override public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();
        exchange.getResponse().getHeaders().set("Cache-Control", "no-store");
        exchange.getResponse().getHeaders().set("X-Content-Type-Options", "nosniff");
        if (method == HttpMethod.GET && path.equals("/actuator/health")) return chain.filter(exchange);
        // WeChat authenticates this exact server-to-server endpoint with its own signature.
        if (path.equals("/api/wechat/callback") && (method == HttpMethod.GET || method == HttpMethod.POST)) {
            return chain.filter(exchange);
        }
        boolean safe = method == HttpMethod.GET || method == HttpMethod.HEAD;
        if (!safe && !sameOrigin(exchange)) return error(exchange, HttpStatus.FORBIDDEN, "Request origin rejected");
        if ((method == HttpMethod.POST && path.equals("/api/auth/login"))
                || (method == HttpMethod.GET && path.equals("/api/auth/session"))) return chain.filter(exchange);

        // Blocking password/Redis work must never run on a Netty event loop.
        return Mono.fromCallable(() -> Optional.ofNullable(auth.authenticate(token(exchange))))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(exception -> error(exchange, HttpStatus.SERVICE_UNAVAILABLE, "Authentication unavailable")
                        .then(Mono.empty()))
                .flatMap(found -> {
                    if (found.isEmpty()) return error(exchange, HttpStatus.UNAUTHORIZED, "Login required");
                    var session = found.get();
                    if (!safe && !csrfMatches(exchange, session)) {
                        return error(exchange, HttpStatus.FORBIDDEN, "Session verification failed");
                    }
                    exchange.getAttributes().put(SESSION_ATTRIBUTE, session);
                    cookie(exchange, token(exchange), session.remembered(), false);
                    return chain.filter(exchange);
                });
    }

    static boolean csrfMatches(ServerWebExchange exchange, OwnerSessionStore.Session session) {
        String supplied = exchange.getRequest().getHeaders().getFirst("X-Cynexus-CSRF");
        return supplied != null && MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),
                session.csrfToken().getBytes(StandardCharsets.UTF_8));
    }

    static boolean sameOrigin(ServerWebExchange exchange) {
        String origin = exchange.getRequest().getHeaders().getOrigin();
        if (origin == null) return false;
        try {
            URI uri = URI.create(origin);
            String host = exchange.getRequest().getHeaders().getFirst("Host");
            return uri.getRawAuthority() != null && uri.getRawAuthority().equalsIgnoreCase(host)
                    && uri.getRawUserInfo() == null && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                    && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && ("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && isLoopback(uri.getHost())));
        } catch (IllegalArgumentException exception) { return false; }
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
    }

    public static String token(ServerWebExchange exchange) {
        var cookie = exchange.getRequest().getCookies().getFirst(COOKIE);
        return cookie == null ? null : cookie.getValue();
    }

    public static void cookie(ServerWebExchange exchange, String token, boolean remembered, boolean clear) {
        boolean secure = !isLoopback(exchange.getRequest().getURI().getHost());
        var cookie = ResponseCookie.from(COOKIE, clear ? "" : token).path("/")
                .httpOnly(true).secure(secure).sameSite("Strict");
        if (clear) cookie.maxAge(0);
        else if (remembered) cookie.maxAge(OwnerAuthService.REMEMBERED_TTL);
        exchange.getResponse().addCookie(cookie.build());
    }

    public static Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().set("Content-Type", "application/json");
        byte[] body = ("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
