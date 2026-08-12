package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.application.dto.ApiWorkflowProcessDTO;
import cn.cccs7.cynexus.core.application.service.ApiWorkflowAppService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/api-workflows")
public class ApiWorkflowController {
    private final ApiWorkflowAppService service;

    public ApiWorkflowController(ApiWorkflowAppService service) { this.service = service; }

    @GetMapping
    public Result<List<ApiWorkflowProcessDTO>> list(@RequestParam(defaultValue = "50") int limit,
                                                    @RequestParam(required = false) String type,
                                                    @RequestParam(required = false) String status) {
        return Result.success(service.list(limit, type, status));
    }
}
