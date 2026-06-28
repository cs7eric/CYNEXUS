package cn.cccs7.cynexus.market.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StockFundFlowCaptureBatchDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-market", contextId = "stockFundFlowCaptureBatchFeignClient", path = "/api/market/stock-fund-flow-capture-batch")
public interface StockFundFlowCaptureBatchFeignClient {

    @GetMapping
    Result<List<StockFundFlowCaptureBatchDTO>> list();

    @GetMapping("/{id}")
    Result<StockFundFlowCaptureBatchDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody StockFundFlowCaptureBatchDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody StockFundFlowCaptureBatchDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
