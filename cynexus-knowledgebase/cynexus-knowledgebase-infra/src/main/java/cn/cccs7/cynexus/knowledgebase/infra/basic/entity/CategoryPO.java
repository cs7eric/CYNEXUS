package cn.cccs7.cynexus.knowledgebase.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_category")
public class CategoryPO extends BasePO {

    private String categoryCode;
    private String categoryName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    private Integer sortOrder;
}
