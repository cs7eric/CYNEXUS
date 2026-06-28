package cn.cccs7.cynexus.market.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StockFundFlowSnapshotDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-market", contextId = "stockFundFlowSnapshotFeignClient", path = "/api/market/stock-fund-flow-snapshot")
public interface StockFundFlowSnapshotFeignClient {

    @GetMapping
    Result<List<StockFundFlowSnapshotDTO>> list();

    @GetMapping("/{id}")
    Result<StockFundFlowSnapshotDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody StockFundFlowSnapshotDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody StockFundFlowSnapshotDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
