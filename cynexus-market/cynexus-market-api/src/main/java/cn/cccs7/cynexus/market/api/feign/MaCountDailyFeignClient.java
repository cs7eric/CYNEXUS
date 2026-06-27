package cn.cccs7.cynexus.market.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MaCountDailyDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-market", path = "/api/market/msi-ma-count-daily")
public interface MaCountDailyFeignClient {

    @GetMapping
    Result<List<MaCountDailyDTO>> list();

    @GetMapping("/{id}")
    Result<MaCountDailyDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody MaCountDailyDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody MaCountDailyDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
