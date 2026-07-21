package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceOverviewDTO;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceTransactionDTO;
import cn.cccs7.cynexus.core.application.request.finance.FinanceTransactionRequest;
import cn.cccs7.cynexus.core.application.service.FinanceLedgerAppService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/finance")
public class FinanceLedgerController {
    private final FinanceLedgerAppService service;

    public FinanceLedgerController(FinanceLedgerAppService service) { this.service = service; }

    @GetMapping("/overview")
    public Result<FinanceOverviewDTO> overview(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) { return Result.success(service.overview(start, end)); }

    @GetMapping("/transactions")
    public Result<List<FinanceTransactionDTO>> transactions(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                            @RequestParam(defaultValue = "100") Integer limit) { return Result.success(service.listTransactions(start, end, limit)); }

    @GetMapping("/transactions/{id}")
    public Result<FinanceTransactionDTO> transaction(@PathVariable Long id) { return Result.success(service.findTransaction(id)); }

    @PostMapping("/transactions")
    public Result<FinanceTransactionDTO> save(@RequestBody FinanceTransactionRequest request) { return Result.success(service.saveTransaction(request)); }

    @org.springframework.web.bind.annotation.PutMapping("/transactions/{id}")
    public Result<FinanceTransactionDTO> update(@PathVariable Long id, @RequestBody FinanceTransactionRequest request) {
        request.setId(id);
        return Result.success(service.saveTransaction(request));
    }

    @DeleteMapping("/transactions/{id}")
    public Result<Boolean> archive(@PathVariable Long id) { return Result.success(service.archiveTransaction(id)); }

    @org.springframework.web.bind.annotation.PostMapping("/transactions/{id}/delete")
    public Result<Boolean> delete(@PathVariable Long id) { return Result.success(service.deleteTransaction(id)); }
}
