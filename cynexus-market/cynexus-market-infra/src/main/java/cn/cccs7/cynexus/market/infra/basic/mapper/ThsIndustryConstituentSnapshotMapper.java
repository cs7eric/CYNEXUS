package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.market.infra.basic.entity.ThsIndustryConstituentSnapshotPO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ThsIndustryConstituentSnapshotMapper extends BaseMapperX<ThsIndustryConstituentSnapshotPO> {

    List<ThsIndustryConstituentSnapshotPO> selectLatestByIndustryCode(@Param("industryCode") String industryCode);

    List<ThsIndustryConstituentSnapshotPO> selectLatestByIndustryCodeOnOrBefore(@Param("industryCode") String industryCode,
                                                                                @Param("snapshotDate") LocalDate snapshotDate);
}
