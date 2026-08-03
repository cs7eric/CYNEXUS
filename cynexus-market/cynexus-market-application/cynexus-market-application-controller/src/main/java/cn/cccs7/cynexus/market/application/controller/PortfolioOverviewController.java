package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.PortfolioOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioAccountDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioTradeDTO;
import cn.cccs7.cynexus.market.api.request.PortfolioAccountUpdateRequest;
import cn.cccs7.cynexus.market.application.service.PortfolioOverviewAppService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/portfolio")
public class PortfolioOverviewController {
    private final PortfolioOverviewAppService service;

    public PortfolioOverviewController(PortfolioOverviewAppService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public Result<PortfolioOverviewDTO> overview() {
        return Result.success(service.overview());
    }

    @GetMapping("/account")
    public Result<PortfolioAccountDTO> account() {
        return Result.success(service.account());
    }

    @PatchMapping("/account")
    public Result<PortfolioAccountDTO> updateAccount(@RequestBody PortfolioAccountUpdateRequest request) {
        return Result.success(service.updateAccount(request));
    }

    @GetMapping("/trades")
    public Result<List<PortfolioTradeDTO>> trades(@RequestParam(required = false) String symbol) {
        return Result.success(service.trades(symbol));
    }
}
