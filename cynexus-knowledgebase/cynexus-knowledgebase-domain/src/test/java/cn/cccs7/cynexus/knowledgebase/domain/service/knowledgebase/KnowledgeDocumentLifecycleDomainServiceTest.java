package cn.cccs7.cynexus.knowledgebase.domain.service.knowledgebase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.index.KnowledgeBaseIndexDomainService;
import cn.cccs7.cynexus.knowledgebase.domain.knowledgebase.KnowledgeBaseDomainService;
import cn.cccs7.cynexus.knowledgebase.domain.service.knowledgebase.impl.KnowledgeDocumentLifecycleDomainServiceImpl;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeDocumentLifecycleDomainServiceTest {

    @Test
    void createsDocumentBeforeLoadingItForIndexRefresh() {
        List<String> calls = new ArrayList<>();
        KnowledgeDocumentBO document = KnowledgeDocumentBO.builder().id(101L).content("content").build();
        KnowledgeDocumentLifecycleDomainService service = newService(calls, document);

        assertEquals(101L, service.createDocument(document));
        assertEquals(List.of("createDocument", "findDocumentById", "refreshDocumentIndex"), calls);
    }

    @Test
    void refreshesOnlySuccessfulDocumentUpdates() {
        List<String> calls = new ArrayList<>();
        KnowledgeDocumentBO document = KnowledgeDocumentBO.builder().id(101L).build();
        KnowledgeDocumentLifecycleDomainService service = newService(calls, document);

        assertTrue(service.updateDocument(document));
        assertEquals(List.of("updateDocument", "findDocumentById", "refreshDocumentIndex"), calls);
    }

    @Test
    void removesIndexOnlyAfterDocumentDeletion() {
        List<String> calls = new ArrayList<>();
        KnowledgeDocumentLifecycleDomainService service = newService(calls, null);

        assertTrue(service.deleteDocument(101L));
        assertEquals(List.of("deleteDocument", "deleteDocumentIndex"), calls);
    }

    @Test
    void delegatesMetadataOperationsThroughTheSameLifecycleEntryPoint() {
        List<String> calls = new ArrayList<>();
        KnowledgeDocumentLifecycleDomainService service = newService(calls, null);

        service.listTags("tag");
        service.createTag(null);
        service.updateTag(null);
        service.deleteTag(1L);
        service.listCategories("category");
        service.createCategory(null);
        service.updateCategory(null);
        service.deleteCategory(2L);

        assertEquals(List.of(
                "listTags", "createTag", "updateTag", "deleteTag",
                "listCategories", "createCategory", "updateCategory", "deleteCategory"), calls);
    }

    private KnowledgeDocumentLifecycleDomainService newService(
            List<String> calls, KnowledgeDocumentBO document) {
        KnowledgeBaseDomainService documentService = proxy(KnowledgeBaseDomainService.class, calls, document);
        KnowledgeBaseIndexDomainService indexService = proxy(KnowledgeBaseIndexDomainService.class, calls, null);
        return new KnowledgeDocumentLifecycleDomainServiceImpl(documentService, indexService);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> contract, List<String> calls, KnowledgeDocumentBO document) {
        return (T) Proxy.newProxyInstance(
                contract.getClassLoader(),
                new Class<?>[]{contract},
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    if (method.getName().equals("createDocument")) {
                        return 101L;
                    }
                    if (method.getName().equals("findDocumentById")) {
                        return document;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return true;
                    }
                    if (method.getReturnType() == List.class) {
                        return List.of();
                    }
                    if (method.getReturnType() == Long.class) {
                        return 101L;
                    }
                    return null;
                });
    }
}
