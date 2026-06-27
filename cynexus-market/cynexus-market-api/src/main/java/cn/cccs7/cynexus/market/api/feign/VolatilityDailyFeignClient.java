package cn.cccs7.cynexus.market.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.VolatilityDailyDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-market", path = "/api/market/msi-volatility-daily")
public interface VolatilityDailyFeignClient {

    @GetMapping
    Result<List<VolatilityDailyDTO>> list();

    @GetMapping("/{id}")
    Result<VolatilityDailyDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody VolatilityDailyDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody VolatilityDailyDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
