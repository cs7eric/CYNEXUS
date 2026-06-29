package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.ThsIndustryConstituentSnapshotPO;
import java.util.List;

public interface ThsIndustryConstituentSnapshotBasicService extends CrudBasicService<ThsIndustryConstituentSnapshotPO> {

    List<ThsIndustryConstituentSnapshotPO> listLatestByIndustryCode(String industryCode);
}
