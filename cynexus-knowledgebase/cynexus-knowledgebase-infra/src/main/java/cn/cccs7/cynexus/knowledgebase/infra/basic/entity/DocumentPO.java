package cn.cccs7.cynexus.knowledgebase.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "kb_document", autoResultMap = true)
public class DocumentPO extends BasePO {

    private String title;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    private String content;
    private String contentFormat;
    private String status;
    private String tenantKey;
    private String ownerKey;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
}
