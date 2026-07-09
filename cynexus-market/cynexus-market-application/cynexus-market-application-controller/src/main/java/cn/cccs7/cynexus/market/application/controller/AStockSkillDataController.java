package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.AStockSkillDataDTO;
import cn.cccs7.cynexus.market.application.service.AStockSkillDataAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/a-stock-skill")
public class AStockSkillDataController {

    private final AStockSkillDataAppService aStockSkillDataAppService;

    public AStockSkillDataController(AStockSkillDataAppService aStockSkillDataAppService) {
        this.aStockSkillDataAppService = aStockSkillDataAppService;
    }

    @GetMapping("/datasets")
    public Result<AStockSkillDataDTO.DatasetListDTO> datasets() {
        return Result.success(aStockSkillDataAppService.listDatasets());
    }

    @GetMapping("/datasets/{datasetCode}/rows")
    public Result<AStockSkillDataDTO.RowsDTO> rows(@PathVariable String datasetCode,
                                                  @RequestParam(defaultValue = "50") Integer limit,
                                                  @RequestParam(required = false) String code,
                                                  @RequestParam(required = false) String source) {
        return Result.success(aStockSkillDataAppService.listRows(datasetCode, limit, code, source));
    }
}
