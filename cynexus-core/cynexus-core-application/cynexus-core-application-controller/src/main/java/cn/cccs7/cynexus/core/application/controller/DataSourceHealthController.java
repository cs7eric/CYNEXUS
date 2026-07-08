package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.application.dto.DataSourceHealthDTO;
import cn.cccs7.cynexus.core.application.service.DataSourceHealthAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/data-source-health")
public class DataSourceHealthController {

    private final DataSourceHealthAppService dataSourceHealthAppService;

    public DataSourceHealthController(DataSourceHealthAppService dataSourceHealthAppService) {
        this.dataSourceHealthAppService = dataSourceHealthAppService;
    }

    @GetMapping("/latest")
    public Result<DataSourceHealthDTO> latest() {
        return Result.success(dataSourceHealthAppService.latestSnapshot());
    }

    @GetMapping("/snapshot")
    public Result<DataSourceHealthDTO> snapshot(
            @RequestParam(value = "includeSlow", defaultValue = "false") Boolean includeSlow) {
        return Result.success(dataSourceHealthAppService.runSnapshot(Boolean.TRUE.equals(includeSlow)));
    }

    @PostMapping("/checks/run")
    public Result<DataSourceHealthDTO> runChecks(
            @RequestParam(value = "includeSlow", defaultValue = "true") Boolean includeSlow) {
        return Result.success(dataSourceHealthAppService.runSnapshot(Boolean.TRUE.equals(includeSlow)));
    }
}
