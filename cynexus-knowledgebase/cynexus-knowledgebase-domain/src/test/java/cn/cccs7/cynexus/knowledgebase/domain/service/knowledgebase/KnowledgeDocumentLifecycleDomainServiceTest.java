package cn.cccs7.cynexus.knowledgebase.domain.service.knowledgebase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.index.KnowledgeBaseIndexDomainService;
import cn.cccs7.cynexus.knowledgebase.domain.index.KnowledgeDocumentIndexEventPublisher;
import cn.cccs7.cynexus.knowledgebase.domain.knowledgebase.KnowledgeBaseDomainService;
import cn.cccs7.cynexus.knowledgebase.domain.service.knowledgebase.impl.KnowledgeDocumentLifecycleDomainServiceImpl;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

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
        KnowledgeDocumentBO existing = KnowledgeDocumentBO.builder().id(101L).tenantKey("tenant-a").ownerKey("owner-a").build();
        KnowledgeDocumentBO document = KnowledgeDocumentBO.builder().id(101L).build();
        KnowledgeDocumentLifecycleDomainService service = newService(calls, existing);

        assertTrue(service.updateDocument(document));
        assertEquals("tenant-a", document.getTenantKey());
        assertEquals("owner-a", document.getOwnerKey());
        assertEquals(List.of("findDocumentById", "updateDocument", "findDocumentById", "refreshDocumentIndex"), calls);
    }

    @Test
    void removesIndexOnlyAfterDocumentDeletion() {
        List<String> calls = new ArrayList<>();
        KnowledgeDocumentLifecycleDomainService service = newService(calls, null);

        assertTrue(service.deleteDocument(101L));
        assertEquals(List.of("deleteDocument", "deleteDocumentIndex"), calls);
    }

    @Test
    void publishesChangedAndDeletedLifecycleEvents() {
        List<String> events = new ArrayList<>();
        KnowledgeDocumentIndexEventPublisher publisher = new KnowledgeDocumentIndexEventPublisher() {
            @Override public void documentChanged(KnowledgeDocumentBO document) { events.add("changed:" + document.getId()); }
            @Override public void documentDeleted(Long documentId) { events.add("deleted:" + documentId); }
        };
        ObjectProvider<KnowledgeDocumentIndexEventPublisher> provider = new ObjectProvider<>() {
            @Override public KnowledgeDocumentIndexEventPublisher getObject(Object... args) { return publisher; }
            @Override public KnowledgeDocumentIndexEventPublisher getIfAvailable() { return publisher; }
            @Override public KnowledgeDocumentIndexEventPublisher getIfUnique() { return publisher; }
            @Override public KnowledgeDocumentIndexEventPublisher getIfAvailable(java.util.function.Supplier<KnowledgeDocumentIndexEventPublisher> supplier) { return publisher; }
            @Override public KnowledgeDocumentIndexEventPublisher getIfUnique(java.util.function.Supplier<KnowledgeDocumentIndexEventPublisher> supplier) { return publisher; }
            @Override public java.util.stream.Stream<KnowledgeDocumentIndexEventPublisher> stream() { return java.util.stream.Stream.of(publisher); }
            @Override public java.util.stream.Stream<KnowledgeDocumentIndexEventPublisher> orderedStream() { return java.util.stream.Stream.of(publisher); }
        };
        KnowledgeDocumentBO document = KnowledgeDocumentBO.builder().id(101L).content("content").build();
        KnowledgeDocumentLifecycleDomainService service = new KnowledgeDocumentLifecycleDomainServiceImpl(
                proxy(KnowledgeBaseDomainService.class, new ArrayList<>(), document),
                proxy(KnowledgeBaseIndexDomainService.class, new ArrayList<>(), null), provider);
        service.createDocument(document);
        service.updateDocument(KnowledgeDocumentBO.builder().id(101L).content("updated").build());
        service.deleteDocument(101L);
        assertEquals(List.of("changed:101", "changed:101", "deleted:101"), events);
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
