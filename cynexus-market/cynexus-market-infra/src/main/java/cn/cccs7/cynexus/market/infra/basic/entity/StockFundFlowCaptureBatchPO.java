package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "stock_fund_flow_capture_batch", autoResultMap = true)
public class StockFundFlowCaptureBatchPO extends BasePO {

    private LocalDate tradeDate;
    private String sourceType;
    private String status;
    private String source;
    private Integer rowCount;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String requestedCodes;
    private OffsetDateTime fetchedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
