package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.market.infra.basic.entity.StockTurnoverSeriesSnapshotPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockTurnoverSeriesSnapshotMapper extends BaseMapperX<StockTurnoverSeriesSnapshotPO> {

    StockTurnoverSeriesSnapshotPO findBySymbol(@Param("symbol") String symbol);
}
