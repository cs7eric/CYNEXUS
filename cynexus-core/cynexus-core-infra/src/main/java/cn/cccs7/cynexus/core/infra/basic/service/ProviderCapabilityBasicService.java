package cn.cccs7.cynexus.core.infra.basic.service;

import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import cn.cccs7.cynexus.core.infra.basic.entity.ProviderCapabilityPO;
import java.util.List;

public interface ProviderCapabilityBasicService extends CrudBasicService<ProviderCapabilityPO> {

    List<ProviderCapabilityPO> listEnabled();
}
