package cn.cccs7.cynexus.ai.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;

import cn.cccs7.cynexus.ai.api.request.KnowledgeBaseSearchRequest;
import cn.cccs7.cynexus.ai.domain.port.KnowledgeRagPort;
import cn.cccs7.cynexus.ai.domain.rag.KnowledgeRagAnswerDomainService;
import org.junit.jupiter.api.Test;
import cn.cccs7.cynexus.ai.application.service.agent.identity.AgentIdentityResolver;
import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;

class KnowledgeBaseSearchAppServiceTest {
    @Test void rejectsMissingIdentity() {
        var request = new KnowledgeBaseSearchRequest();
        request.setQuery("hello");
        assertThrows(RuntimeException.class, () -> new KnowledgeBaseSearchAppService(mock(KnowledgeRagPort.class), mock(KnowledgeRagAnswerDomainService.class)).search(request));
    }

    @Test void trustedHeaderIdentityOverridesSpoofedBodyIdentity() {
        KnowledgeRagPort rag = mock(KnowledgeRagPort.class);
        var answer = mock(KnowledgeRagAnswerDomainService.class);
        var resolver = new AgentIdentityResolver("test-secret", false);
        var service = new KnowledgeBaseSearchAppService(rag, answer, resolver);
        var request = new KnowledgeBaseSearchRequest();
        request.setTenantKey("spoofed-tenant");
        request.setPrincipalKey("spoofed-user");
        request.setQuery("hello");
        String signature = TrustedPrincipalSigner.sign("trusted-user", "trusted-tenant", "test-secret");
        service.search(request, "trusted-user", "trusted-tenant", signature);
        verify(rag).search(argThat(value -> "trusted-tenant".equals(value.tenantKey())
                && "trusted-user".equals(value.principalKey())));
    }
}
