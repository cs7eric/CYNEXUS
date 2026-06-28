package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.ProfitEffectDailyDTO;
import cn.cccs7.cynexus.market.application.service.ProfitEffectDailyCrudAppService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/msi-profit-effect-daily")
public class ProfitEffectDailyCrudController {

    private final ProfitEffectDailyCrudAppService appService;

    public ProfitEffectDailyCrudController(ProfitEffectDailyCrudAppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public Result<List<ProfitEffectDailyDTO>> list() {
        return Result.success(appService.list());
    }

    @GetMapping("/{id}")
    public Result<ProfitEffectDailyDTO> get(@PathVariable("id") Long id) {
        return Result.success(appService.findById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody ProfitEffectDailyDTO dto) {
        return Result.success(appService.create(dto));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody ProfitEffectDailyDTO dto) {
        return Result.success(appService.update(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        return Result.success(appService.delete(id));
    }
}
