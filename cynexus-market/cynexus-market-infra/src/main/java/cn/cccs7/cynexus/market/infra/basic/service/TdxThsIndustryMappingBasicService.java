package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.TdxThsIndustryMappingPO;
import java.util.List;

public interface TdxThsIndustryMappingBasicService extends CrudBasicService<TdxThsIndustryMappingPO> {

    List<TdxThsIndustryMappingPO> listByTdxCode6(String tdxCode6);
}
