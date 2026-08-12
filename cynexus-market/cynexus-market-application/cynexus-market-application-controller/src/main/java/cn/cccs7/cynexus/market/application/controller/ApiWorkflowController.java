package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.ApiWorkflowPageDTO;
import cn.cccs7.cynexus.market.application.service.ApiWorkflowAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/api-workflows")
public class ApiWorkflowController {
    private final ApiWorkflowAppService service;

    public ApiWorkflowController(ApiWorkflowAppService service) { this.service = service; }

    @GetMapping
    public Result<ApiWorkflowPageDTO> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(service.list(page, pageSize));
    }
}
