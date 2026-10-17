package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.KnowledgeBaseSearchResultDTO;
import cn.cccs7.cynexus.ai.api.request.KnowledgeBaseSearchRequest;
import cn.cccs7.cynexus.ai.domain.port.KnowledgeRagPort;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import cn.cccs7.cynexus.ai.api.dto.KnowledgeBaseAnswerDTO;
import cn.cccs7.cynexus.ai.api.request.KnowledgeBaseAnswerRequest;
import cn.cccs7.cynexus.ai.domain.rag.KnowledgeRagAnswerDomainService;
import cn.cccs7.cynexus.ai.domain.rag.KnowledgeRagSearchPolicy;
import cn.cccs7.cynexus.ai.application.service.agent.identity.AgentIdentityResolver;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.ObjectProvider;

@Service
@ConditionalOnProperty(prefix = "cynexus.ai.rag", name = "enabled", havingValue = "true")
public class KnowledgeBaseSearchAppService {
    private final ObjectProvider<KnowledgeRagPort> ragProvider;
    private final ObjectProvider<KnowledgeRagAnswerDomainService> answerServiceProvider;
    private final KnowledgeRagAnswerDomainService directAnswerService;
    private final AgentIdentityResolver identityResolver;
    private final KnowledgeRagSearchPolicy searchPolicy = new KnowledgeRagSearchPolicy();
    public KnowledgeBaseSearchAppService(ObjectProvider<KnowledgeRagPort> ragProvider, ObjectProvider<KnowledgeRagAnswerDomainService> answerServiceProvider) {
        this(ragProvider, answerServiceProvider, null);
    }
    @Autowired
    public KnowledgeBaseSearchAppService(ObjectProvider<KnowledgeRagPort> ragProvider, ObjectProvider<KnowledgeRagAnswerDomainService> answerServiceProvider,
                                         AgentIdentityResolver identityResolver) {
        this.ragProvider = ragProvider; this.answerServiceProvider = answerServiceProvider; this.directAnswerService = null; this.identityResolver = identityResolver;
    }

    /** Compatibility constructor for unit tests and non-Spring callers. */
    public KnowledgeBaseSearchAppService(KnowledgeRagPort rag, KnowledgeRagAnswerDomainService answerService) {
        this.ragProvider = new SingletonProvider<>(rag);
        this.answerServiceProvider = new SingletonProvider<>(answerService);
        this.directAnswerService = answerService;
        this.identityResolver = null;
    }

    /** Compatibility constructor for trusted-identity unit tests. */
    public KnowledgeBaseSearchAppService(KnowledgeRagPort rag, KnowledgeRagAnswerDomainService answerService,
                                         AgentIdentityResolver identityResolver) {
        this.ragProvider = new SingletonProvider<>(rag);
        this.answerServiceProvider = new SingletonProvider<>(answerService);
        this.directAnswerService = answerService;
        this.identityResolver = identityResolver;
    }

    public KnowledgeBaseAnswerDTO answer(KnowledgeBaseAnswerRequest request, String principal, String tenant, String signature) {
        AgentIdentity identity = requireTrustedIdentity(principal, tenant, signature);
        return answer(withIdentity(request, identity));
    }

    public KnowledgeBaseAnswerDTO answer(KnowledgeBaseAnswerRequest request) {
        KnowledgeRagAnswerDomainService answerService = directAnswerService != null ? directAnswerService : answerServiceProvider.getIfAvailable();
        if (answerService == null) throw new BizException(ErrorCode.INTERNAL_ERROR, "Knowledge RAG vector store is not configured");
        var answer = answerService.answer(new KnowledgeRagAnswerDomainService.Command(request == null ? null : request.getTenantKey(), request == null ? null : request.getPrincipalKey(), request == null ? null : request.getQuestion(), request == null || request.getTopK() == null ? 8 : request.getTopK(), request == null ? null : request.getPreferredModelCode(), request == null ? null : request.getPermissionScope()));
        return KnowledgeBaseAnswerDTO.builder().content(answer.content()).sources(answer.sources().stream().map(item -> KnowledgeBaseSearchResultDTO.builder().documentId(item.documentId()).content(item.content()).score(item.score()).metadata(item.metadata()).build()).toList()).build();
    }

    public List<KnowledgeBaseSearchResultDTO> search(KnowledgeBaseSearchRequest request) {
        KnowledgeRagPort rag = requireRag();
        KnowledgeRagSearchPolicy.SearchRequest normalized;
        try {
            normalized = searchPolicy.normalize(
                    request == null ? null : request.getTenantKey(),
                    request == null ? null : request.getPrincipalKey(),
                    request == null ? null : request.getQuery(),
                    request == null ? null : request.getTopK(),
                    request == null ? null : request.getPermissionScope());
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, exception.getMessage());
        }
        return rag.search(new KnowledgeRagPort.SearchRequest(normalized.tenantKey(), normalized.principalKey(),
                        normalized.query(), normalized.topK(), normalized.permissionScope()))
                .stream().map(item -> KnowledgeBaseSearchResultDTO.builder().documentId(item.documentId()).content(item.content())
                        .score(item.score()).metadata(item.metadata()).build()).toList();
    }

    public List<KnowledgeBaseSearchResultDTO> search(KnowledgeBaseSearchRequest request, String principal,
                                                     String tenant, String signature) {
        AgentIdentity identity = requireTrustedIdentity(principal, tenant, signature);
        return search(withIdentity(request, identity));
    }

    private AgentIdentity requireTrustedIdentity(String principal, String tenant, String signature) {
        if (identityResolver == null) throw new BizException(ErrorCode.INTERNAL_ERROR, "Trusted identity resolver is not configured");
        try {
            return identityResolver.resolve(principal, tenant, signature);
        } catch (RuntimeException exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, "A trusted tenant and principal identity is required");
        }
    }

    private KnowledgeBaseSearchRequest withIdentity(KnowledgeBaseSearchRequest source, AgentIdentity identity) {
        KnowledgeBaseSearchRequest copy = source == null ? new KnowledgeBaseSearchRequest() : source;
        copy.setTenantKey(identity.getTenantKey());
        copy.setPrincipalKey(identity.getPrincipalKey());
        return copy;
    }

    private KnowledgeBaseAnswerRequest withIdentity(KnowledgeBaseAnswerRequest source, AgentIdentity identity) {
        KnowledgeBaseAnswerRequest copy = source == null ? new KnowledgeBaseAnswerRequest() : source;
        copy.setTenantKey(identity.getTenantKey());
        copy.setPrincipalKey(identity.getPrincipalKey());
        return copy;
    }
    private KnowledgeRagPort requireRag() {
        KnowledgeRagPort rag = ragProvider.getIfAvailable();
        if (rag == null) throw new BizException(ErrorCode.INTERNAL_ERROR, "Knowledge RAG vector store is not configured");
        return rag;
    }

    private static final class SingletonProvider<T> implements ObjectProvider<T> {
        private final T value;
        private SingletonProvider(T value) { this.value = value; }
        public T getObject() { return value; }
        public T getObject(Object... args) { return value; }
        public T getIfAvailable() { return value; }
        public T getIfUnique() { return value; }
        public java.util.Iterator<T> iterator() { return java.util.Collections.singleton(value).iterator(); }
        public java.util.stream.Stream<T> stream() { return java.util.stream.Stream.of(value); }
        public java.util.stream.Stream<T> orderedStream() { return stream(); }
    }
}
