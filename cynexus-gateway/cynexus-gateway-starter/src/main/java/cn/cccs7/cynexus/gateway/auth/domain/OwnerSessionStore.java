package cn.cccs7.cynexus.gateway.auth.domain;

import java.time.Duration;

public interface OwnerSessionStore {
    record Session(String csrfToken, boolean remembered) {}
    void checkLoginRate(String client);
    void create(String tokenHash, Session session, Duration ttl);
    Session touch(String tokenHash, Duration rememberedTtl, Duration temporaryTtl);
    void revoke(String tokenHash);
    void revokeAll();
}
