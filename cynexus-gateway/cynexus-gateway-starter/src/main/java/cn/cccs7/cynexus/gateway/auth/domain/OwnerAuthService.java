package cn.cccs7.cynexus.gateway.auth.domain;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

public final class OwnerAuthService {
    public static final Duration REMEMBERED_TTL = Duration.ofDays(30);
    public static final Duration TEMPORARY_TTL = Duration.ofHours(12);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final OwnerCredentials credentials;
    private final OwnerSessionStore sessions;

    public record Login(String token, OwnerSessionStore.Session session) {}
    public static final class LoginRejected extends RuntimeException {}
    public static final class RateLimited extends RuntimeException {}

    public OwnerAuthService(OwnerCredentials credentials, OwnerSessionStore sessions) {
        this.credentials = credentials;
        this.sessions = sessions;
    }

    public Login login(String password, boolean remember, String client, String previousToken) {
        sessions.checkLoginRate(client);
        if (!credentials.matches(password)) throw new LoginRejected();
        String token = randomToken();
        var session = new OwnerSessionStore.Session(randomToken(), remember);
        sessions.create(OwnerCredentials.digest(token), session, remember ? REMEMBERED_TTL : TEMPORARY_TTL);
        logout(previousToken);
        return new Login(token, session);
    }

    public OwnerSessionStore.Session authenticate(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return null;
        return sessions.touch(OwnerCredentials.digest(token), REMEMBERED_TTL, TEMPORARY_TTL);
    }

    public void logout(String token) {
        if (token != null && token.matches("[A-Za-z0-9_-]{43}")) sessions.revoke(OwnerCredentials.digest(token));
    }

    public void logoutAll() { sessions.revokeAll(); }

    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
