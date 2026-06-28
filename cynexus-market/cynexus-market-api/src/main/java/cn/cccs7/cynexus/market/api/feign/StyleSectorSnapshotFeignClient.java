package cn.cccs7.cynexus.market.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StyleSectorSnapshotDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-market", contextId = "styleSectorSnapshotFeignClient", path = "/api/market/mkt-style-sector-snapshot")
public interface StyleSectorSnapshotFeignClient {

    @GetMapping
    Result<List<StyleSectorSnapshotDTO>> list();

    @GetMapping("/{id}")
    Result<StyleSectorSnapshotDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody StyleSectorSnapshotDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody StyleSectorSnapshotDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
