package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.application.dto.ProviderCapabilityDTO;
import cn.cccs7.cynexus.core.application.service.ProviderCapabilityAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/provider-capabilities")
public class ProviderCapabilityController {

    private final ProviderCapabilityAppService providerCapabilityAppService;

    public ProviderCapabilityController(ProviderCapabilityAppService providerCapabilityAppService) {
        this.providerCapabilityAppService = providerCapabilityAppService;
    }

    @GetMapping
    public Result<ProviderCapabilityDTO> catalog() {
        return Result.success(providerCapabilityAppService.catalog());
    }
}
