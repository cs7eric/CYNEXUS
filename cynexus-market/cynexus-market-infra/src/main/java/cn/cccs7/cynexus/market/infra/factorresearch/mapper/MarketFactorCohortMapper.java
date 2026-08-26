package cn.cccs7.cynexus.market.infra.factorresearch.mapper;

import cn.cccs7.cynexus.market.infra.factorresearch.entity.MarketFactorEventFactPO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MarketFactorCohortMapper {
    List<MarketFactorEventFactPO> listLimitUpTouches(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate, @Param("limit") int limit);
    List<MarketFactorEventFactPO> listLargeMoveCloses(@Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate,
                                                       @Param("minChangePct") BigDecimal minChangePct,
                                                       @Param("maxChangePct") BigDecimal maxChangePct,
                                                       @Param("limit") int limit);
}
