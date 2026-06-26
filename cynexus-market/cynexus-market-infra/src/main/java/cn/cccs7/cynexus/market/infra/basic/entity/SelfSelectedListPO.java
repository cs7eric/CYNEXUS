package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "self_selected_lists", autoResultMap = true)
public class SelfSelectedListPO extends BasePO {

    private String legacyKey;
    private String name;
    private String description;
    private String color;
    private String listKind;
    private String status;
    private Integer sortOrder;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
