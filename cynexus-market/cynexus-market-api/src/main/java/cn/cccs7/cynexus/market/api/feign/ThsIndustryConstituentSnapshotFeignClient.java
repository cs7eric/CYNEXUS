package cn.cccs7.cynexus.market.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.ThsIndustryConstituentSnapshotDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-market", contextId = "thsIndustryConstituentSnapshotFeignClient", path = "/api/market/ths-industry-constituent-snapshot")
public interface ThsIndustryConstituentSnapshotFeignClient {

    @GetMapping
    Result<List<ThsIndustryConstituentSnapshotDTO>> list();

    @GetMapping("/{id}")
    Result<ThsIndustryConstituentSnapshotDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody ThsIndustryConstituentSnapshotDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody ThsIndustryConstituentSnapshotDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
