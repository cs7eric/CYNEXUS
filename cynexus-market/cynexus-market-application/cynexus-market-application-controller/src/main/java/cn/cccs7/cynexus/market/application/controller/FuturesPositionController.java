package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.FuturesPositionDTO;
import cn.cccs7.cynexus.market.application.service.FuturesPositionQueryAppService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/futures-position")
public class FuturesPositionController {
    private final FuturesPositionQueryAppService service;

    public FuturesPositionController(FuturesPositionQueryAppService service) {
        this.service = service;
    }

    @GetMapping("/latest")
    public Result<FuturesPositionDTO.Latest> latest(@RequestParam(required = false) String tradeDate,
                                                    @RequestParam(defaultValue = "CONTRACT") String scopeType,
                                                    @RequestParam(required = false) String exchangeCode,
                                                    @RequestParam(required = false) String varietyCode,
                                                    @RequestParam(defaultValue = "10") int top,
                                                    @RequestParam(defaultValue = "netPosition") String sort,
                                                    @RequestParam(defaultValue = "desc") String sortDir,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer pageSize) {
        return Result.success(service.latest(tradeDate, scopeType, exchangeCode, varietyCode, top, sort, sortDir, page, pageSize));
    }

    @GetMapping("/instruments")
    public Result<FuturesPositionDTO.Latest> instruments(@RequestParam(required = false) String tradeDate,
                                                         @RequestParam(defaultValue = "CONTRACT") String scopeType,
                                                         @RequestParam(required = false) String exchangeCode,
                                                         @RequestParam(required = false) String varietyCode,
                                                         @RequestParam(defaultValue = "10") int top,
                                                         @RequestParam(defaultValue = "netPosition") String sort,
                                                         @RequestParam(defaultValue = "desc") String sortDir,
                                                         @RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer pageSize) {
        return Result.success(service.instruments(tradeDate, scopeType, exchangeCode, varietyCode, top, sort, sortDir, page, pageSize));
    }

    @GetMapping("/{scopeType}/{exchangeCode}/{instrumentCode}/members")
    public Result<List<FuturesPositionDTO.Member>> members(@PathVariable String scopeType,
                                                           @PathVariable String exchangeCode,
                                                           @PathVariable String instrumentCode,
                                                           @RequestParam(required = false) String tradeDate,
                                                           @RequestParam(required = false) String rankType,
                                                           @RequestParam(defaultValue = "20") int top) {
        return Result.success(service.members(tradeDate, scopeType, exchangeCode, instrumentCode, rankType, top));
    }

    @GetMapping("/{scopeType}/{exchangeCode}/{instrumentCode}/history")
    public Result<List<FuturesPositionDTO.History>> history(@PathVariable String scopeType,
                                                             @PathVariable String exchangeCode,
                                                             @PathVariable String instrumentCode,
                                                             @RequestParam(required = false) String tradeDate,
                                                             @RequestParam(defaultValue = "20") int top,
                                                             @RequestParam(defaultValue = "120") int limit) {
        return Result.success(service.history(tradeDate, scopeType, exchangeCode, instrumentCode, top, limit));
    }

    @GetMapping("/coverage")
    public Result<FuturesPositionDTO.Coverage> coverage(@RequestParam(required = false) String tradeDate) {
        return Result.success(service.coverage(tradeDate));
    }
}
