package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.market.infra.basic.entity.StockBasicPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockBasicMapper extends BaseMapperX<StockBasicPO> {

    List<StockBasicPO> searchForStockChart(@Param("keyword") String keyword, @Param("limit") int limit);

    StockBasicPO findByCodeForStockChart(@Param("code") String code);
}
