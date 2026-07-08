package cn.cccs7.cynexus.knowledgebase.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "kb_document_index_task", autoResultMap = true)
public class DocumentIndexTaskPO extends BasePO {

    private Long documentId;
    private String target;
    private String status;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String payload;
    private String errorMessage;
}
