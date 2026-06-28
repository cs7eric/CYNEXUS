package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sec_stock_sector")
public class StockSectorPO extends BasePO {

    private String code;
    private String sectorId;
    private BigDecimal relationLevel;
    private String reason;
    private String source;
}
