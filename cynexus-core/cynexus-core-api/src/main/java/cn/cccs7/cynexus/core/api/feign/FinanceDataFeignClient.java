package cn.cccs7.cynexus.core.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceOverviewDTO;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceTransactionDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cynexus-core", contextId = "financeDataFeignClient")
public interface FinanceDataFeignClient {
    @GetMapping("/api/core/finance/overview")
    Result<FinanceOverviewDTO> getOverview(
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end);

    @GetMapping("/api/core/finance/transactions")
    Result<List<FinanceTransactionDTO>> getTransactions(
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(value = "limit", defaultValue = "50") Integer limit);
}
