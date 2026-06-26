package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.convert.CoreServiceInfoDTOConverter;
import cn.cccs7.cynexus.core.application.dto.CoreServiceInfoDTO;
import cn.cccs7.cynexus.core.domain.service.CoreServiceInfoDomainService;
import org.springframework.stereotype.Service;

@Service
public class CoreInfoAppService {

    private final CoreServiceInfoDomainService coreServiceInfoDomainService;

    public CoreInfoAppService(CoreServiceInfoDomainService coreServiceInfoDomainService) {
        this.coreServiceInfoDomainService = coreServiceInfoDomainService;
    }

    public CoreServiceInfoDTO getCoreInfo() {
        return CoreServiceInfoDTOConverter.INSTANCE.toSource(coreServiceInfoDomainService.getServiceInfo());
    }
}
