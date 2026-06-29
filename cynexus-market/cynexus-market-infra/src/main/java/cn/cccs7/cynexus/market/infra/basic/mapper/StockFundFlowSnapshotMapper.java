package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.market.infra.basic.entity.StockFundFlowSnapshotPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockFundFlowSnapshotMapper extends BaseMapperX<StockFundFlowSnapshotPO> {

    StockFundFlowSnapshotPO findLatestByCode(@Param("code") String code);

    List<StockFundFlowSnapshotPO> listRecentByCode(@Param("code") String code, @Param("limit") int limit);
}
