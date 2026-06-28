package cn.cccs7.cynexus.market.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StyleRiskDailyDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-market", contextId = "styleRiskDailyFeignClient", path = "/api/market/msi-style-risk-daily")
public interface StyleRiskDailyFeignClient {

    @GetMapping
    Result<List<StyleRiskDailyDTO>> list();

    @GetMapping("/{id}")
    Result<StyleRiskDailyDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody StyleRiskDailyDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody StyleRiskDailyDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
