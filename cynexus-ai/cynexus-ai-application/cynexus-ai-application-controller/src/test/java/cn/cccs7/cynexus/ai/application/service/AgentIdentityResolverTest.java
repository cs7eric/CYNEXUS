package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.service.agent.identity.AgentIdentityResolver;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import org.junit.jupiter.api.Test;

class AgentIdentityResolverTest {

    private static final String SECRET = "test-agent-identity-secret-01234567890123456789";

    @Test
    void acceptsOnlyGatewaySignedIdentity() {
        AgentIdentityResolver resolver = new AgentIdentityResolver(SECRET, false);
        String signature = TrustedPrincipalSigner.sign("user-1", "tenant-1", SECRET);

        AgentIdentity identity = resolver.resolve("user-1", "tenant-1", signature);

        assertEquals("tenant-1::user-1", identity.storageOwnerKey());
        assertThrows(IllegalStateException.class,
                () -> resolver.resolve("user-2", "tenant-1", signature));
        assertThrows(IllegalStateException.class,
                () -> resolver.resolve("user-1", "tenant-2", signature));
    }

    @Test
    void anonymousCompatibilityIsExplicit() {
        AgentIdentityResolver resolver = new AgentIdentityResolver("", true);
        assertEquals("default::single-user", resolver.resolve(null, null, null).storageOwnerKey());
        assertThrows(IllegalStateException.class,
                () -> new AgentIdentityResolver("", false).resolve(null, null, null));
    }

    @Test
    void governanceRequiresConfiguredAdministratorPrincipal() {
        AgentIdentityResolver resolver = new AgentIdentityResolver(SECRET, false, "admin,workspace-user");
        String adminSignature = TrustedPrincipalSigner.sign("admin", "tenant-1", SECRET);
        assertEquals("tenant-1::admin", resolver.resolveGovernance("admin", "tenant-1", adminSignature).storageOwnerKey());

        String userSignature = TrustedPrincipalSigner.sign("user-1", "tenant-1", SECRET);
        assertThrows(IllegalStateException.class,
                () -> resolver.resolveGovernance("user-1", "tenant-1", userSignature));
    }
}
