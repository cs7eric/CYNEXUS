package cn.cccs7.cynexus.market.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.ProfitEffectDailyDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-market", contextId = "profitEffectDailyFeignClient", path = "/api/market/msi-profit-effect-daily")
public interface ProfitEffectDailyFeignClient {

    @GetMapping
    Result<List<ProfitEffectDailyDTO>> list();

    @GetMapping("/{id}")
    Result<ProfitEffectDailyDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody ProfitEffectDailyDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody ProfitEffectDailyDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
