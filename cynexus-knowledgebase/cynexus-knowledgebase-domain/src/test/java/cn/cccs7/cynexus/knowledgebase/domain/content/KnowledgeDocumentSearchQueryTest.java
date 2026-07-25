package cn.cccs7.cynexus.knowledgebase.domain.content;

import cn.cccs7.cynexus.knowledgebase.common.constant.KnowledgeBaseConstants;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeDocumentSearchQueryTest {

    @Test
    void normalizesStatusAndFindsChineseMultiTermDocument() {
        KnowledgeDocumentSearchQuery query = KnowledgeDocumentSearchQuery.of(
                "A股 数据 落库 表设计 已发布 文档", "PUBLISHED", null, null);

        assertEquals("published", query.getStatus());
        assertTrue(query.getTokens().contains("数据"));
        assertTrue(query.getTokens().contains("落库"));
        assertTrue(query.rank(List.of(
                KnowledgeDocumentBO.builder().title("无关文档").content("其他内容").build(),
                KnowledgeDocumentBO.builder().title("A Stock 数据落库表设计").content("已发布文档").build()))
                .get(0).getTitle().contains("数据落库"));
    }

    @Test
    void boundsAndDeduplicatesProviderSearchInput() {
        String input = " 数据   数据   " + "x".repeat(500);
        KnowledgeDocumentSearchQuery query = KnowledgeDocumentSearchQuery.of(input, " published ", null, null);

        assertEquals("published", query.getStatus());
        assertTrue(query.getKeyword().length() <= KnowledgeBaseConstants.MAX_SEARCH_KEYWORD_LENGTH);
        assertTrue(query.getTokens().size() <= KnowledgeBaseConstants.MAX_SEARCH_TOKENS);
    }
}
