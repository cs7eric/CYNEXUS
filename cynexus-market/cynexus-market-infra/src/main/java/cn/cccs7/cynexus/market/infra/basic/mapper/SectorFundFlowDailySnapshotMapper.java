package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorFundFlowDailySnapshotPO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SectorFundFlowDailySnapshotMapper extends BaseMapperX<SectorFundFlowDailySnapshotPO> {

    LocalDate findLatestTradeDate(@Param("scope") String scope, @Param("maxTradeDate") LocalDate maxTradeDate);

    List<SectorFundFlowDailySnapshotPO> listByTradeDate(@Param("scope") String scope,
                                                        @Param("tradeDate") LocalDate tradeDate,
                                                        @Param("limit") int limit);

    List<SectorFundFlowDailySnapshotPO> listRecent(@Param("scope") String scope,
                                                   @Param("maxTradeDate") LocalDate maxTradeDate,
                                                   @Param("days") int days,
                                                   @Param("top") int top);
}
