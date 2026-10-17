package cn.cccs7.cynexus.gateway.auth;

import cn.cccs7.cynexus.gateway.auth.domain.*;
import cn.cccs7.cynexus.gateway.TrustedPrincipalGatewayFilter;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

class OwnerAuthTest {
    private MemorySessions store;
    private OwnerAuthService auth;
    private OwnerCredentials credentials;

    @BeforeEach void setup() throws Exception {
        byte[] salt = new byte[16];
        var key = new PBEKeySpec("test-owner-password".toCharArray(), salt, 310000, 256);
        String hash = HexFormat.of().formatHex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(key).getEncoded());
        credentials = new OwnerCredentials("pbkdf2-sha256:310000:" + HexFormat.of().formatHex(salt) + ":" + hash);
        store = new MemorySessions();
        auth = new OwnerAuthService(credentials, store);
    }

    @Test void passwordAndConfigurationValidation() {
        assertTrue(credentials.matches("test-owner-password"));
        assertFalse(credentials.matches("incorrect"));
        assertFalse(credentials.matches("x".repeat(1025)));
        assertThrows(IllegalArgumentException.class, () -> new OwnerCredentials(""));
        assertThrows(OwnerAuthService.LoginRejected.class, () -> auth.login("wrong", true, "client", null));
        assertEquals(1, store.attempts);
    }

    @Test void rememberedAndTemporaryDevicesAndRevocation() {
        var first = auth.login("test-owner-password", true, "client", null);
        assertEquals(Duration.ofDays(30), store.ttl);
        assertNotEquals(first.token(), first.session().csrfToken());
        assertNotNull(auth.authenticate(first.token()));
        var second = auth.login("test-owner-password", false, "client", first.token());
        assertEquals(Duration.ofHours(12), store.ttl);
        assertNull(auth.authenticate(first.token()));
        assertNotNull(auth.authenticate(second.token()));
        assertNull(auth.authenticate("malformed"));
        auth.logoutAll();
        assertNull(auth.authenticate(second.token()));
    }

    @Test void everyRouteIncludingStreamsDataAndManagementIsProtected() {
        for (String path : new String[]{"/api/core/scheduler/management/jobs", "/api/ai/agent/stream", "/data-api/anything", "/actuator/gateway/routes"}) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost:18000" + path).header("Host", "localhost:18000"));
            new OwnerAuthWebFilter(auth).filter(exchange, next -> fail("Unauthenticated request reached downstream")).block();
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode(), path);
        }
    }

    @Test void secureCookieAndCsrfAndSlidingRenewal() {
        var login = auth.login("test-owner-password", true, "client", null);
        var filter = new OwnerAuthWebFilter(auth);
        for (String origin : new String[]{"https://evil.example", "null", "https://app.example"}) {
            var request = MockServerHttpRequest.post("https://app.example/api/core/update")
                    .header("Host", "app.example").header("Origin", origin)
                    .cookie(new org.springframework.http.HttpCookie(OwnerAuthWebFilter.COOKIE, login.token()));
            var exchange = MockServerWebExchange.from(request);
            filter.filter(exchange, next -> fail("CSRF request reached downstream")).block();
            assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        }
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("https://app.example/api/core/update")
                .header("Host", "app.example").header("Origin", "https://app.example")
                .header("X-Cynexus-CSRF", login.session().csrfToken())
                .cookie(new org.springframework.http.HttpCookie(OwnerAuthWebFilter.COOKIE, login.token())));
        filter.filter(exchange, next -> { next.getResponse().setStatusCode(HttpStatus.NO_CONTENT); return Mono.empty(); }).block();
        assertEquals(HttpStatus.NO_CONTENT, exchange.getResponse().getStatusCode());
        var cookie = exchange.getResponse().getCookies().getFirst(OwnerAuthWebFilter.COOKIE);
        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(Duration.ofDays(30), cookie.getMaxAge());
        assertEquals(2, store.touches);
    }

    @Test void loginRequiresSameOriginAndHealthIsNarrowlyPublic() {
        var filter = new OwnerAuthWebFilter(auth);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("http://localhost:18000/api/auth/login").header("Host", "localhost:18000"));
        filter.filter(exchange, next -> fail("Missing origin accepted")).block();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        var health = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost:18000/actuator/health"));
        filter.filter(health, next -> { next.getResponse().setStatusCode(HttpStatus.OK); return Mono.empty(); }).block();
        assertEquals(HttpStatus.OK, health.getResponse().getStatusCode());
    }

    @Test void redisFailureFailsClosed() {
        var login = auth.login("test-owner-password", true, "client", null);
        store.unavailable = true;
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost:18000/api/core/data")
                .cookie(new org.springframework.http.HttpCookie(OwnerAuthWebFilter.COOKIE, login.token())));
        new OwnerAuthWebFilter(auth).filter(exchange, next -> fail("Redis failure bypassed auth")).block();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    @Test void sessionCookiesAreNotPersistedWhenRememberIsOff() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost:5174/"));
        OwnerAuthWebFilter.cookie(exchange, OwnerAuthService.randomToken(), false, false);
        var cookie = exchange.getResponse().getCookies().getFirst(OwnerAuthWebFilter.COOKIE);
        assertNotNull(cookie);
        assertFalse(cookie.isSecure());
        assertTrue(cookie.getMaxAge().isNegative());
    }

    @Test void onlyExactWechatCallbackUsesProviderAuthentication() {
        for (HttpMethod method : new HttpMethod[]{HttpMethod.GET, HttpMethod.POST}) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.method(method,
                    "http://localhost:18000/api/wechat/callback"));
            new OwnerAuthWebFilter(auth).filter(exchange, next -> {
                assertNull(next.getAttribute(OwnerAuthWebFilter.SESSION_ATTRIBUTE));
                next.getResponse().setStatusCode(HttpStatus.OK);
                return Mono.empty();
            }).block();
            assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
        }
        for (String path : new String[]{"/api/wechat/history", "/api/wechat/callback/extra"}) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost:18000" + path));
            new OwnerAuthWebFilter(auth).filter(exchange, next -> fail("Webhook exemption was too broad")).block();
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        }
    }

    @Test void identityAssertionsRequireOwnerAuthenticationAndDiscardClientHeaders() {
        var signer = new TrustedPrincipalGatewayFilter("workspace-user", "default", "x".repeat(48));
        for (boolean authenticated : new boolean[]{false, true}) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost:18000/api/wechat/callback")
                    .header("X-Cynexus-Principal", "forged").header("X-Cynexus-Tenant", "forged")
                    .header("X-Cynexus-Identity-Signature", "forged"));
            if (authenticated) exchange.getAttributes().put(OwnerAuthWebFilter.SESSION_ATTRIBUTE,
                    new OwnerSessionStore.Session(OwnerAuthService.randomToken(), true));
            signer.filter(exchange, next -> {
                var headers = next.getRequest().getHeaders();
                assertEquals(authenticated ? "workspace-user" : null, headers.getFirst("X-Cynexus-Principal"));
                assertEquals(authenticated ? "default" : null, headers.getFirst("X-Cynexus-Tenant"));
                if (authenticated) assertNotEquals("forged", headers.getFirst("X-Cynexus-Identity-Signature"));
                else assertNull(headers.getFirst("X-Cynexus-Identity-Signature"));
                return Mono.empty();
            }).block();
        }
    }

    private static final class MemorySessions implements OwnerSessionStore {
        private final Map<String, Session> sessions = new HashMap<>();
        private Duration ttl;
        private int attempts;
        private int touches;
        private boolean unavailable;
        @Override public void checkLoginRate(String client) { attempts++; }
        @Override public void create(String key, Session session, Duration ttl) { sessions.put(key, session); this.ttl = ttl; }
        @Override public Session touch(String key, Duration rememberedTtl, Duration temporaryTtl) {
            if (unavailable) throw new IllegalStateException("Unavailable");
            var session = sessions.get(key);
            if (session != null) { touches++; ttl = session.remembered() ? rememberedTtl : temporaryTtl; }
            return session;
        }
        @Override public void revoke(String key) { sessions.remove(key); }
        @Override public void revokeAll() { sessions.clear(); }
    }
}
