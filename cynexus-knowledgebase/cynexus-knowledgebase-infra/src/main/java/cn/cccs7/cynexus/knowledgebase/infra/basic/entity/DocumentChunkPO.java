package cn.cccs7.cynexus.knowledgebase.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "kb_document_chunk", autoResultMap = true)
public class DocumentChunkPO extends BasePO {

    private Long documentId;
    private Integer chunkIndex;
    private String heading;
    private String content;
    private String contentHash;
    private Integer tokenEstimate;
    private String embeddingProvider;
    private String embeddingModel;
    private String embeddingStatus;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String embeddingVectorJson;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
}
