package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.market.infra.basic.entity.TdxThsIndustryMappingPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TdxThsIndustryMappingMapper extends BaseMapperX<TdxThsIndustryMappingPO> {

    List<TdxThsIndustryMappingPO> selectByTdxCode6(@Param("tdxCode6") String tdxCode6);
}
