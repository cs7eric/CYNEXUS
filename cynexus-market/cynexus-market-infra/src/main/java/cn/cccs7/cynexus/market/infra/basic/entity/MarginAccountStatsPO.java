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
@TableName(value = "margin_account_stats", autoResultMap = true)
public class MarginAccountStatsPO extends BasePO {

    private LocalDate tradeDate;
    private String source;
    private BigDecimal financingBalance;
    private BigDecimal securitiesLendingBalance;
    private BigDecimal financingBuyAmount;
    private BigDecimal securitiesLendingSellAmount;
    private Integer brokerCount;
    private Integer businessDepartmentCount;
    private Integer personalInvestorCount;
    private Integer institutionInvestorCount;
    private Integer activeInvestorCount;
    private Integer liabilityInvestorCount;
    private BigDecimal totalGuarantee;
    private BigDecimal averageGuaranteeRatio;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
}
