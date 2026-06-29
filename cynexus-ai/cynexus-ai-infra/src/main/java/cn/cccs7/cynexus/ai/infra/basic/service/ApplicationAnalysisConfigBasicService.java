package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisConfigPO;
import cn.cccs7.cynexus.common.db.service.CrudBasicService;

public interface ApplicationAnalysisConfigBasicService extends CrudBasicService<ApplicationAnalysisConfigPO> {

    ApplicationAnalysisConfigPO getDefaultActive();
}
