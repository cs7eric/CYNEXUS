package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sec_sector")
public class SectorPO extends BasePO {

    private String sectorId;
    private Integer categoryRaw;
    private String categoryLabel;
    private String name;
    private String topicId;
    private String source;
}
