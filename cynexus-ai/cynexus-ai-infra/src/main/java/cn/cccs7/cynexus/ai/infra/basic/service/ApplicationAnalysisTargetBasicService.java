package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisTargetPO;
import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import java.time.OffsetDateTime;
import java.util.List;

public interface ApplicationAnalysisTargetBasicService extends CrudBasicService<ApplicationAnalysisTargetPO> {

    List<ApplicationAnalysisTargetPO> listActiveTargets();

    ApplicationAnalysisTargetPO getActiveByTargetKey(String targetKey);

    int softDeleteMissing(List<String> targetKeys, OffsetDateTime deletedAt);
}
