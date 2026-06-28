package cn.cccs7.cynexus.market.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.TdxIndustryQuoteSnapshotDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-market", contextId = "tdxIndustryQuoteSnapshotFeignClient", path = "/api/market/tdx-industry-quote-snapshot")
public interface TdxIndustryQuoteSnapshotFeignClient {

    @GetMapping
    Result<List<TdxIndustryQuoteSnapshotDTO>> list();

    @GetMapping("/{id}")
    Result<TdxIndustryQuoteSnapshotDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody TdxIndustryQuoteSnapshotDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody TdxIndustryQuoteSnapshotDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
