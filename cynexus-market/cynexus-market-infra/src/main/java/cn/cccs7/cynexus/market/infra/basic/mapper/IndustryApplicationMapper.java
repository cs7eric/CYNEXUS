package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationConfigPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationResultPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationTargetPO;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IndustryApplicationMapper {

    IndustryApplicationConfigPO findConfig(@Param("configKey") String configKey);

    int upsertConfig(@Param("value") IndustryApplicationConfigPO value);

    List<IndustryApplicationTargetPO> listTargets();

    IndustryApplicationTargetPO findTarget(@Param("targetKey") String targetKey);

    int softDeleteAllTargets(@Param("now") OffsetDateTime now);

    int upsertTarget(@Param("value") IndustryApplicationTargetPO value);

    IndustryApplicationResultPO findResult(@Param("targetKey") String targetKey);

    int upsertResult(@Param("value") IndustryApplicationResultPO value);
}
