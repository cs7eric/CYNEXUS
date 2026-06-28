package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tdx_industry_quote_batch")
public class TdxIndustryQuoteBatchPO extends BasePO {

    private String batchKey;
    private LocalDate snapshotDate;
    private String source;
    private Integer rowCount;
    private OffsetDateTime fetchedAt;
}
