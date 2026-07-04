package cn.cccs7.cynexus.core.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobCategoryMappingPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SchedulerJobCategoryMappingMapper extends BaseMapperX<SchedulerJobCategoryMappingPO> {

    List<SchedulerJobCategoryMappingPO> selectByJobId(@Param("jobId") Long jobId);
}
