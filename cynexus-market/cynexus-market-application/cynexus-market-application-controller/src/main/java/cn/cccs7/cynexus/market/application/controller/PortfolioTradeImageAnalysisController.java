package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.PortfolioTradeImageAnalysisDTO;
import cn.cccs7.cynexus.market.application.service.PortfolioTradeImageAnalysisAppService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/market/portfolio/trade-analysis")
public class PortfolioTradeImageAnalysisController {
    private final PortfolioTradeImageAnalysisAppService service;

    public PortfolioTradeImageAnalysisController(PortfolioTradeImageAnalysisAppService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<PortfolioTradeImageAnalysisDTO> analyze(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return Result.success(service.analyze(image != null ? image : file));
    }
}
