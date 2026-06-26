package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.application.dto.CoreServiceInfoDTO;
import cn.cccs7.cynexus.core.application.service.CoreInfoAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core")
public class CoreInfoController {

    private final CoreInfoAppService coreInfoAppService;

    public CoreInfoController(CoreInfoAppService coreInfoAppService) {
        this.coreInfoAppService = coreInfoAppService;
    }

    @GetMapping("/info")
    public Result<CoreServiceInfoDTO> getCoreInfo() {
        return Result.success(coreInfoAppService.getCoreInfo());
    }
}
