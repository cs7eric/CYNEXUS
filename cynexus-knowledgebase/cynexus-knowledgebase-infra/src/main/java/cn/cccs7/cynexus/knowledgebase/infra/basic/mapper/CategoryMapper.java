package cn.cccs7.cynexus.knowledgebase.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.CategoryPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapperX<CategoryPO> {
}
