package cn.cccs7.cynexus.market.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.RiskAppetiteDailyDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-market", contextId = "riskAppetiteDailyFeignClient", path = "/api/market/msi-risk-appetite-daily")
public interface RiskAppetiteDailyFeignClient {

    @GetMapping
    Result<List<RiskAppetiteDailyDTO>> list();

    @GetMapping("/{id}")
    Result<RiskAppetiteDailyDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody RiskAppetiteDailyDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody RiskAppetiteDailyDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
