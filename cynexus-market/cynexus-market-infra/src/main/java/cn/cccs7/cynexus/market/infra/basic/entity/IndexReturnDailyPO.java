package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "mkt_index_return_daily", autoResultMap = true)
public class IndexReturnDailyPO extends BasePO {

    private LocalDate tradeDate;
    private String indexCode;
    private String indexName;
    private Integer windowDays;
    private BigDecimal currentClose;
    private LocalDate currentTradeDate;
    private BigDecimal baseClose;
    private LocalDate baseDate;
    private BigDecimal returnPct;
    private String source;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
