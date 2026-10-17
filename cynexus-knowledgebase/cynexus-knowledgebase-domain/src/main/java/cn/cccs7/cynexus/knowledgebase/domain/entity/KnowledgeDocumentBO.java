package cn.cccs7.cynexus.knowledgebase.domain.entity;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentBO {

    private Long id;
    private String title;
    private String description;
    private String content;
    private String contentFormat;
    private String status;
    private String extra;
    private String tenantKey;
    private String ownerKey;
    private List<Long> tagIds;
    private List<Long> categoryIds;
    private List<KnowledgeTagBO> tags;
    private List<KnowledgeCategoryBO> categories;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
