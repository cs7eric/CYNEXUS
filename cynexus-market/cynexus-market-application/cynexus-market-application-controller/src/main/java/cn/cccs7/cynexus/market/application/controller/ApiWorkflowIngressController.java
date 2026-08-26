package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportEnqueueDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportTaskDTO;
import cn.cccs7.cynexus.market.application.service.PortfolioImportAppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/workflows")
public class ApiWorkflowIngressController {

    private final PortfolioImportAppService service;

    public ApiWorkflowIngressController(PortfolioImportAppService service) {
        this.service = service;
    }

    @PostMapping(value = "/portfolio-trade-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<PortfolioImportEnqueueDTO> portfolioTradeImport(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PortfolioImportTaskDTO task = service.upload(file, idempotencyKey);
        return Result.success(PortfolioImportEnqueueDTO.builder()
                .accepted(true)
                .message("queued")
                .processId(String.valueOf(task.getId()))
                .status("queued")
                .workflowType("PORTFOLIO_TRADE_IMPORT")
                .workflowKey("portfolio.trade.import.screenshot")
                .pollUrl("/api/market/portfolio/imports/" + task.getId())
                .build());
    }
}
