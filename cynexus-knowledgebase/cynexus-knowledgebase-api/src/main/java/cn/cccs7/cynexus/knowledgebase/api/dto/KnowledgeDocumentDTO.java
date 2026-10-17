package cn.cccs7.cynexus.knowledgebase.api.dto;

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
public class KnowledgeDocumentDTO {

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
    private List<KnowledgeTagDTO> tags;
    private List<KnowledgeCategoryDTO> categories;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
