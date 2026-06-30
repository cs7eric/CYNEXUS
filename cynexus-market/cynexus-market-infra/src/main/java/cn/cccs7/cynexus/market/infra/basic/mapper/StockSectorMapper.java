package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.market.infra.basic.entity.StockSectorConstituentPO;
import cn.cccs7.cynexus.market.infra.basic.entity.StockSectorJoinedPO;
import cn.cccs7.cynexus.market.infra.basic.entity.StockSectorPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockSectorMapper extends BaseMapperX<StockSectorPO> {

    List<StockSectorJoinedPO> listJoinedByCode(@Param("code") String code);

    List<StockSectorConstituentPO> listConstituentsBySector(@Param("sectorId") String sectorId,
                                                            @Param("categoryRaw") Integer categoryRaw,
                                                            @Param("topicId") String topicId,
                                                            @Param("name") String name,
                                                            @Param("limit") Integer limit);
}
