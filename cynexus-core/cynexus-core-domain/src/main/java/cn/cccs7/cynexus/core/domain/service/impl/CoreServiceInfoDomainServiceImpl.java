package cn.cccs7.cynexus.core.domain.service.impl;

import cn.cccs7.cynexus.core.common.constant.CoreConstants;
import cn.cccs7.cynexus.core.domain.entity.CoreServiceInfoBO;
import cn.cccs7.cynexus.core.domain.service.CoreServiceInfoDomainService;

public class CoreServiceInfoDomainServiceImpl implements CoreServiceInfoDomainService {

    @Override
    public CoreServiceInfoBO getServiceInfo() {
        return new CoreServiceInfoBO(
                CoreConstants.SERVICE_NAME,
                CoreConstants.DEFAULT_SCHEMA,
                "Core platform service for shared scheduler ownership and future cross-service foundations"
        );
    }
}
