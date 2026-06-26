package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.application.convert.CoreServiceInfoDTOConverter;
import cn.cccs7.cynexus.core.application.dto.CoreServiceInfoDTO;
import cn.cccs7.cynexus.core.domain.entity.CoreServiceInfoBO;
import cn.cccs7.cynexus.core.domain.service.CoreServiceInfoDomainService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core")
public class CoreInfoController {

    private final CoreServiceInfoDomainService coreServiceInfoDomainService;

    public CoreInfoController(CoreServiceInfoDomainService coreServiceInfoDomainService) {
        this.coreServiceInfoDomainService = coreServiceInfoDomainService;
    }

    @GetMapping("/info")
    public Result<CoreServiceInfoDTO> getCoreInfo() {
        CoreServiceInfoBO bo = coreServiceInfoDomainService.getServiceInfo();
        return Result.success(CoreServiceInfoDTOConverter.INSTANCE.toSource(bo));
    }
}
