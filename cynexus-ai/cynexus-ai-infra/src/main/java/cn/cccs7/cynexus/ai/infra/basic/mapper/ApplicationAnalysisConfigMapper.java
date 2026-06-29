package cn.cccs7.cynexus.ai.infra.basic.mapper;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisConfigPO;
import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApplicationAnalysisConfigMapper extends BaseMapperX<ApplicationAnalysisConfigPO> {

    ApplicationAnalysisConfigPO selectDefaultActive();
}
