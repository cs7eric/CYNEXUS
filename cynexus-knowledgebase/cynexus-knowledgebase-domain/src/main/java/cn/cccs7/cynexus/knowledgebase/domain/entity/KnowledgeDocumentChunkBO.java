package cn.cccs7.cynexus.knowledgebase.domain.entity;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentChunkBO {

    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String heading;
    private String content;
    private String contentHash;
    private Integer tokenEstimate;
    private String embeddingProvider;
    private String embeddingModel;
    private String embeddingStatus;
    private String embeddingVectorJson;
    private String extra;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
