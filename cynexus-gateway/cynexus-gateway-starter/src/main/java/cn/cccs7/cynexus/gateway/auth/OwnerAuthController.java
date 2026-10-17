package cn.cccs7.cynexus.gateway.auth;

import cn.cccs7.cynexus.gateway.auth.domain.OwnerAuthService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/auth")
public final class OwnerAuthController {
    private final OwnerAuthService auth;
    public record LoginRequest(String password, Boolean rememberDevice) {}
    public OwnerAuthController(OwnerAuthService auth) { this.auth = auth; }

    @PostMapping(value = "/login", consumes = "application/json")
    public Mono<Void> login(@RequestBody LoginRequest request, ServerWebExchange exchange) {
        String client = exchange.getRequest().getRemoteAddress() == null ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        return Mono.fromCallable(() -> auth.login(request.password(), !Boolean.FALSE.equals(request.rememberDevice()),
                        client, OwnerAuthWebFilter.token(exchange)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(login -> {
                    OwnerAuthWebFilter.cookie(exchange, login.token(), login.session().remembered(), false);
                    exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                    return exchange.getResponse().setComplete();
                }).onErrorResume(OwnerAuthService.LoginRejected.class,
                        exception -> OwnerAuthWebFilter.error(exchange, HttpStatus.UNAUTHORIZED, "Password incorrect"))
                .onErrorResume(OwnerAuthService.RateLimited.class, exception -> {
                    exchange.getResponse().getHeaders().set("Retry-After", "900");
                    return OwnerAuthWebFilter.error(exchange, HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts");
                }).onErrorResume(exception -> OwnerAuthWebFilter.error(exchange,
                        HttpStatus.SERVICE_UNAVAILABLE, "Authentication unavailable"));
    }

    @GetMapping("/session")
    public Mono<Map<String, Object>> session(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            var session = auth.authenticate(OwnerAuthWebFilter.token(exchange));
            if (session == null) return Map.<String, Object>of("authenticated", false);
            OwnerAuthWebFilter.cookie(exchange, OwnerAuthWebFilter.token(exchange), session.remembered(), false);
            return Map.<String, Object>of("authenticated", true, "csrfToken", session.csrfToken(),
                    "rememberDevice", session.remembered());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/logout")
    public Mono<Void> logout(ServerWebExchange exchange) { return revoke(exchange, false); }

    @PostMapping("/logout-all")
    public Mono<Void> logoutAll(ServerWebExchange exchange) { return revoke(exchange, true); }

    private Mono<Void> revoke(ServerWebExchange exchange, boolean all) {
        return Mono.fromRunnable(() -> {
            if (all) auth.logoutAll(); else auth.logout(OwnerAuthWebFilter.token(exchange));
            // The filter may already have added a renewal cookie; replace it on logout.
            exchange.getResponse().getCookies().remove(OwnerAuthWebFilter.COOKIE);
            OwnerAuthWebFilter.cookie(exchange, "", false, true);
            exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
