package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.DataSourceFreshnessDTO;
import cn.cccs7.cynexus.market.application.service.DataSourceFreshnessAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/data-source")
public class DataSourceFreshnessController {

    private final DataSourceFreshnessAppService dataSourceFreshnessAppService;

    public DataSourceFreshnessController(DataSourceFreshnessAppService dataSourceFreshnessAppService) {
        this.dataSourceFreshnessAppService = dataSourceFreshnessAppService;
    }

    @GetMapping("/freshness")
    public Result<DataSourceFreshnessDTO> getFreshness() {
        return Result.success(dataSourceFreshnessAppService.snapshot());
    }
}
