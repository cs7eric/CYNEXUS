package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.PortfolioReviewDTO;
import cn.cccs7.cynexus.market.application.service.PortfolioReviewAppService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/portfolio/reviews")
public class PortfolioReviewController {
    private final PortfolioReviewAppService service;
    public PortfolioReviewController(PortfolioReviewAppService service) { this.service = service; }
    @GetMapping public Result<List<PortfolioReviewDTO>> list(@RequestParam(required = false) String symbol) { return Result.success(service.list(symbol)); }
    @GetMapping("/{id}") public Result<PortfolioReviewDTO> get(@PathVariable long id) { return Result.success(service.get(id)); }
    @PostMapping public Result<PortfolioReviewDTO> save(@RequestBody PortfolioReviewDTO dto) { return Result.success(service.save(dto)); }
}
