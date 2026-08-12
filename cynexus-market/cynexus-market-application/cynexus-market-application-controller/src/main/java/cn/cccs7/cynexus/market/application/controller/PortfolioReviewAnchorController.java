package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.PortfolioReviewAnchorDTO;
import cn.cccs7.cynexus.market.application.service.PortfolioReviewAnchorAppService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/portfolio/review-anchors")
public class PortfolioReviewAnchorController {
    private final PortfolioReviewAnchorAppService service;

    public PortfolioReviewAnchorController(PortfolioReviewAnchorAppService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<PortfolioReviewAnchorDTO>> list(@RequestParam String symbol,
                                                        @RequestParam(name = "trade_date", required = false) LocalDate tradeDate) {
        return Result.success(service.list(symbol, tradeDate));
    }

    @PostMapping
    public Result<PortfolioReviewAnchorDTO> save(@RequestBody PortfolioReviewAnchorDTO dto) {
        return Result.success(service.save(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable long id) {
        service.delete(id);
        return Result.success(Boolean.TRUE);
    }
}
