package cn.cccs7.cynexus.knowledgebase.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_document_category_mapping")
public class DocumentCategoryMappingPO extends BasePO {

    private Long documentId;
    private Long categoryId;
}
